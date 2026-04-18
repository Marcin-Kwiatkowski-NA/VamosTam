package com.vamigo.architecture;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import com.vamigo.VamigoApplication;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mapstruct.Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RestController;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;

/**
 * ArchUnit layering rules — plain JUnit, no Spring context.
 *
 * <p>Rules target constraints CLAUDE.md already promises: controllers carry a known
 * suffix, live inside the application package, every {@code *ServiceImpl} backs a
 * {@code *Service} interface, and field injection is banned outside MapStruct-generated
 * abstract mappers (where the framework requires it).
 *
 * <p>The draft plan also specified "com.vamigo.domain.. must not depend on
 * org.springframework..". That rule is intentionally omitted here: the current codebase
 * keeps a few {@code @Component} helpers in {@code com.vamigo.domain}
 * ({@code ExternalImportSupport}, {@code PersonDisplayNameResolver},
 * {@code ResponseEnricher}). Enforcing the rule would lock in broken state, so it is
 * left as a separate cleanup ticket rather than a silently-failing guard.
 */
class LayeringRulesTest {

    private static JavaClasses classes;

    @BeforeAll
    static void importClasses() throws Exception {
        // Surefire on Windows wraps the classpath in a manifest-only jar, so ArchUnit's
        // default classpath + package scans return zero classes. Resolve the main-classes
        // root from a known class's ProtectionDomain and feed it as a filesystem path.
        URL mainClassesRoot = VamigoApplication.class.getProtectionDomain()
                .getCodeSource().getLocation();
        Path root = Paths.get(mainClassesRoot.toURI());
        classes = new ClassFileImporter().importPath(root);
        if (classes.stream().findAny().isEmpty()) {
            throw new IllegalStateException(
                    "ArchUnit imported 0 classes from " + root + ".");
        }
    }

    @Test
    void controllerClassesAreIdentifiableBySuffix() {
        ArchRule rule = classes()
                .that().areAnnotatedWith(RestController.class)
                .or().areAnnotatedWith(Controller.class)
                .should().haveSimpleNameEndingWith("Controller")
                .orShould().haveSimpleNameEndingWith("Handler")
                .because("controllers and exception handlers must be identifiable by suffix");

        rule.check(classes);
    }

    @Test
    void controllerClassesLiveUnderVamigoRoot() {
        ArchRule rule = classes()
                .that().areAnnotatedWith(RestController.class)
                .or().areAnnotatedWith(Controller.class)
                .should().resideInAPackage("com.vamigo..")
                .because("controllers must live inside the application package tree");

        rule.check(classes);
    }

    @Test
    void everyServiceImplBacksAMatchingServiceInterface() {
        ArchCondition<JavaClass> backsServiceInterface = new ArchCondition<>(
                "implement a com.vamigo..*Service interface") {
            @Override
            public void check(JavaClass cls, ConditionEvents events) {
                boolean ok = cls.getRawInterfaces().stream().anyMatch(i ->
                        i.getPackageName().startsWith("com.vamigo")
                        && i.getSimpleName().matches("[A-Z]\\w*Service"));
                if (!ok) {
                    events.add(SimpleConditionEvent.violated(cls,
                            cls.getName() + " does not implement a matching *Service interface"));
                }
            }
        };

        ArchRule rule = classes()
                .that().haveSimpleNameEndingWith("ServiceImpl")
                .and().resideInAPackage("com.vamigo..")
                .should(backsServiceInterface);

        rule.check(classes);
    }

    @Test
    void noAutowiredFieldsOutsideMapStructMappers() {
        // Accept MapStruct @Mapper abstract classes AND the *MapperImpl subclasses
        // MapStruct generates from them (the generated subclass inherits @Autowired
        // fields but is not itself annotated with @Mapper).
        ArchRule rule = fields()
                .that().areAnnotatedWith(Autowired.class)
                .should().beDeclaredInClassesThat(
                        com.tngtech.archunit.base.DescribedPredicate.describe(
                                "annotated with @Mapper or a generated *MapperImpl",
                                (JavaClass cls) -> cls.isAnnotatedWith(Mapper.class)
                                        || cls.getSimpleName().endsWith("MapperImpl")))
                .because("constructor injection only, except for MapStruct-generated abstract mappers and their generated impls");

        rule.check(classes);
    }
}
