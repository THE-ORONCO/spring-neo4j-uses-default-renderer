package the.oronco.springneo4jusesdefaultrenderer;

import org.neo4j.cypherdsl.core.Cypher;
import org.neo4j.cypherdsl.core.Statement;
import org.neo4j.cypherdsl.core.renderer.Configuration;
import org.neo4j.cypherdsl.core.renderer.Dialect;
import org.neo4j.cypherdsl.core.renderer.Renderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.neo4j.core.Neo4jTemplate;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

import java.lang.reflect.Field;
import java.util.Objects;

@SpringBootApplication
public class SpringNeo4jUsesDefaultRendererApplication {

    Logger log = LoggerFactory.getLogger(SpringNeo4jUsesDefaultRendererApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(SpringNeo4jUsesDefaultRendererApplication.class, args);
    }

    @Bean
    Configuration config() {
        return Configuration.newConfig()
                            .withDialect(Dialect.NEO4J_5)
                            .build();
    }
    public static final String THING_LABEL = "THING";


    @Bean
    CommandLineRunner runner(Neo4jTemplate template, Configuration config) {
        return args -> {
            var thingNode = Cypher.node(THING_LABEL);
            var cypherStatement = Statement.builder()
                    .match(thingNode)
                    .where(Cypher.elementId(thingNode).eq(Cypher.literalOf("test")))
                    .returning(thingNode)
                                           .build();
            var result = template.find(Thing.class)
                    .matching(cypherStatement)
                    .all();

            Field internalField = cypherStatement.getClass() // SinglePartQueryWithResult
                                                 .getSuperclass() // SinglePartQuery
                                                 .getSuperclass() // AbstractStatement
                                                 .getDeclaredField("cypher");
            internalField.setAccessible(true);
            String actualCypher = (String) internalField.get(cypherStatement);

            String expectedCypherStatement = Renderer.getRenderer(config)
                                                     .render(cypherStatement);
            log.warn("===================================================================");
            log.warn("expected statement:\t{}", expectedCypherStatement);
            log.warn("actual statement:\t{}", actualCypher);
            log.warn("===================================================================");

            if (!Objects.equals(expectedCypherStatement, actualCypher))
                throw new Exception("the two statements should be equal!");
        };
    }

    @Node(primaryLabel = THING_LABEL)
    class Thing{

        @Id
        @GeneratedValue
        String id;
        String name;
    }
}
