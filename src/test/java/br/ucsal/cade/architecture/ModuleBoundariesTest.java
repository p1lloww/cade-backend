package br.ucsal.cade.architecture;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

import java.util.Map;
import java.util.Set;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * Fronteiras do monólito modular (D10). Um módulo só usa outro pela API pública ({@code api}), só depende dos
 * módulos permitidos, não há ciclos e as camadas dentro do módulo apontam num só sentido.
 */
@AnalyzeClasses(packages = ModuleBoundariesTest.ROOT, importOptions = ImportOption.DoNotIncludeTests.class)
class ModuleBoundariesTest {

    static final String ROOT = "br.ucsal.cade";

    /**
     * Dependências permitidas entre módulos, como na D10. Qualquer outra é recusada, mesmo pela api: para avisar
     * que algo aconteceu, o módulo publica um evento.
     */
    private static final Map<String, Set<String>> ALLOWED = Map.ofEntries(
            Map.entry("access", Set.of()),
            Map.entry("founditems", Set.of()),
            Map.entry("lossreports", Set.of()),
            Map.entry("matching", Set.of("founditems", "lossreports")),
            Map.entry("pickup", Set.of("founditems", "lossreports", "matching")),
            Map.entry("notifications", Set.of("access", "lossreports", "pickup")),
            Map.entry("retention", Set.of("founditems")),
            Map.entry("publicpage", Set.of("founditems")),
            Map.entry("administration", Set.of("access")),
            Map.entry("demo", Set.of("access", "founditems", "lossreports")));

    /** Um módulo só alcança outro pelo pacote {@code api}. */
    @ArchTest
    static final ArchRule modulesOnlyUseTheApiOfOtherModules = classes().should(onlyUseOtherModulesThroughApi());

    /** Um módulo só depende dos módulos permitidos na D10. */
    @ArchTest
    static final ArchRule modulesOnlyDependOnTheAllowedModules = classes().should(onlyDependOnAllowedModules());

    /** Sem ciclos entre módulos. */
    @ArchTest
    static final ArchRule noCyclesBetweenModules = slices()
            .matching(ROOT + ".(*)..")
            .should().beFreeOfCycles();

    /** O núcleo compartilhado não conhece nenhum módulo de negócio. */
    @ArchTest
    static final ArchRule sharedDoesNotDependOnModules = noClasses()
            .that().resideInAPackage(ROOT + ".shared..")
            .should().dependOnClassesThat().resideInAnyPackage(moduleRoots());

    /** A persistência não conhece serviços nem controllers. */
    @ArchTest
    static final ArchRule persistenceIsTheLowestLayer = noClasses()
            .that().resideInAPackage("..persistence..")
            .should().dependOnClassesThat().resideInAnyPackage("..service..", "..controller..");

    /** Os serviços não conhecem os controllers. */
    @ArchTest
    static final ArchRule servicesDoNotUseControllers = noClasses()
            .that().resideInAPackage("..service..")
            .should().dependOnClassesThat().resideInAPackage("..controller..");

    /** A API pública de um módulo não expõe o que é interno a ele. */
    @ArchTest
    static final ArchRule apiDoesNotDependOnInternals = noClasses()
            .that().resideInAPackage("..api..")
            .should().dependOnClassesThat().resideInAnyPackage("..service..", "..persistence..", "..controller..");

    private static String[] moduleRoots() {
        return ALLOWED.keySet().stream().map(module -> ROOT + "." + module + "..").toArray(String[]::new);
    }

    private static String moduleOf(JavaClass javaClass) {
        String name = javaClass.getPackageName();
        String prefix = ROOT + ".";
        if (!name.startsWith(prefix)) {
            return null;
        }
        String module = name.substring(prefix.length()).split("[.]")[0];
        return ALLOWED.containsKey(module) ? module : null;
    }

    /** O pacote {@code api} do módulo ou um subpacote dele, nunca, por exemplo, {@code apiinterna}. */
    private static boolean isApiPackage(String packageName, String module) {
        String api = ROOT + "." + module + ".api";
        return packageName.equals(api) || packageName.startsWith(api + ".");
    }

    private static ArchCondition<JavaClass> onlyDependOnAllowedModules() {
        return new ArchCondition<>("depender só dos módulos permitidos na D10") {
            @Override
            public void check(JavaClass origin, ConditionEvents events) {
                String from = moduleOf(origin);
                if (from == null) {
                    return;
                }
                origin.getDirectDependenciesFromSelf().forEach(dependency -> {
                    String to = moduleOf(dependency.getTargetClass());
                    if (to != null && !to.equals(from) && !ALLOWED.get(from).contains(to)) {
                        events.add(SimpleConditionEvent.violated(dependency,
                                from + " -> " + to + " não é permitido: " + dependency.getDescription()));
                    }
                });
            }
        };
    }

    private static ArchCondition<JavaClass> onlyUseOtherModulesThroughApi() {
        return new ArchCondition<>("usar outros módulos só pelo pacote api") {
            @Override
            public void check(JavaClass origin, ConditionEvents events) {
                String from = moduleOf(origin);
                if (from == null) {
                    return;
                }
                origin.getDirectDependenciesFromSelf().forEach(dependency -> {
                    JavaClass target = dependency.getTargetClass();
                    String to = moduleOf(target);
                    if (to != null && !to.equals(from) && !isApiPackage(target.getPackageName(), to)) {
                        events.add(SimpleConditionEvent.violated(dependency, dependency.getDescription()));
                    }
                });
            }
        };
    }
}
