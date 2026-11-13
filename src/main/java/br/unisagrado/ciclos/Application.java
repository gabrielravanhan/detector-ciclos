package br.unisagrado.ciclos;

import br.unisagrado.ciclos.instancia.InstanciaService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Ponto de entrada. Uso:
 * <pre>java -jar target/detector-ciclos.jar [--porta=7070] [--instancias=instancias]</pre>
 * Os valores padrão ficam em application.yaml.
 */
@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        ConfigurableApplicationContext contexto = SpringApplication.run(Application.class, args);

        String porta = contexto.getEnvironment().getProperty("local.server.port");
        InstanciaService instancias = contexto.getBean(InstanciaService.class);
        System.out.println("Detector de ciclos rodando em http://localhost:" + porta);
        System.out.println("Instâncias: " + instancias.pasta().toAbsolutePath() + " (" + instancias.listar().size() + " arquivos)");
    }
}
