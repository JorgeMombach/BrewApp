package dev.brewapp.arquitetura;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import java.time.Clock;

@AnalyzeClasses(packages = "dev.brewapp", importOptions = ImportOption.DoNotIncludeTests.class)
class RegrasDeArquiteturaTest {

    private static final String PACOTE_DO_RELOGIO = "dev.brewapp.shared.time..";
    private static final String PACOTE_DE_PERSISTENCIA = "..adapter.out.persistence..";

    /**
     * Domínio é Java puro (spec 4.3). Vale também para o pacote raiz do shared (núcleo compartilhado,
     * usado pelo domínio dos módulos); a infraestrutura do shared fica nos subpacotes.
     */
    @ArchTest
    static final ArchRule dominioNaoDependeDeFramework = noClasses()
            .that().resideInAPackage("..domain..")
            .or().resideInAPackage("dev.brewapp.shared")
            .should().dependOnClassesThat().resideInAnyPackage("org.springframework..", "org.jooq..")
            .because("o domínio e o núcleo compartilhado são Java puro, sem Spring nem jOOQ (spec 4.3)");

    /**
     * jOOQ só na persistência (spec 4.3): records e tabelas geradas não chegam a casos de uso, domínio nem web.
     * As classes geradas ficam em ..adapter.out.persistence.jooq de cada módulo, dentro do pacote permitido.
     */
    @ArchTest
    static final ArchRule jooqSoNaPersistencia = noClasses()
            .that().resideOutsideOfPackage(PACOTE_DE_PERSISTENCIA)
            .should().dependOnClassesThat().resideInAnyPackage("org.jooq..", "..adapter.out.persistence.jooq..")
            .because("os tipos do jOOQ não saem da camada de persistência; as conversões acontecem no adapter");

    /** Tempo sempre pelo Clock injetado (spec 10): now() só é aceito recebendo um Clock. */
    @ArchTest
    static final ArchRule tempoSoPeloClockInjetado = noClasses()
            .should().callMethodWhere(chamadaDeNowSemClock())
            .because("o tempo vem do bean java.time.Clock injetado, para manter os testes determinísticos");

    /** O relógio do sistema só é criado na configuração do bean Clock. */
    @ArchTest
    static final ArchRule relogioDoSistemaSoNaConfiguracao = noClasses()
            .that().resideOutsideOfPackage(PACOTE_DO_RELOGIO)
            .should().callMethodWhere(chamadaDeRelogioDoSistema())
            .because("o Clock do sistema é criado uma vez só, como bean, em " + PACOTE_DO_RELOGIO);

    private static DescribedPredicate<JavaMethodCall> chamadaDeNowSemClock() {
        return DescribedPredicate.describe("now() de java.time sem Clock", javaMethodCall ->
                javaMethodCall.getTargetOwner().getPackageName().equals("java.time")
                        && javaMethodCall.getName().equals("now")
                        && javaMethodCall.getTarget().getRawParameterTypes().stream()
                                .noneMatch(javaClass -> javaClass.isEquivalentTo(Clock.class)));
    }

    private static DescribedPredicate<JavaMethodCall> chamadaDeRelogioDoSistema() {
        return DescribedPredicate.describe("Clock.system*()", javaMethodCall ->
                javaMethodCall.getTargetOwner().isEquivalentTo(Clock.class)
                        && javaMethodCall.getName().startsWith("system"));
    }
}
