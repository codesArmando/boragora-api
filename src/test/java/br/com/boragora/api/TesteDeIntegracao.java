package br.com.boragora.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Aplicação completa com MockMvc e MySQL do Testcontainers. Todas as classes com esta
 * anotação compartilham o mesmo contexto e o mesmo container. Os dados gravados pelas
 * chamadas à API persistem entre os testes, então cada teste usa e-mails próprios.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
public @interface TesteDeIntegracao {
}
