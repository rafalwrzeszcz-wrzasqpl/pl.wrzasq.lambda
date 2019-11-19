/*
 * This file is part of the pl.wrzasq.lambda.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2018 - 2019 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package test.pl.wrzasq.lambda.json;

import java.io.IOException;
import java.time.LocalDate;
import java.time.Month;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import pl.wrzasq.lambda.json.ObjectMapperFactory;

public class ObjectMapperFactoryTest {
    @Test
    public void handleJava8TimeApiSerialization() throws IOException {
        var objectMapper = ObjectMapperFactory.createObjectMapper();

        var date = LocalDate.of(2011, Month.JANUARY, 30);

        var json = objectMapper.writeValueAsString(date);

        Assertions.assertEquals(
            "\"2011-01-30\"",
            json,
            "ObjectMapprFactory.createObjectMapper() should create ObjectMapper capable of Java 8 Time API serialization."
        );
    }

    @Test
    public void handleJava8TimeApiDeserialization() throws IOException {
        var objectMapper = ObjectMapperFactory.createObjectMapper();

        var date = objectMapper.readValue("\"2015-07-02\"", LocalDate.class);

        Assertions.assertEquals(
            2015,
            date.getYear(),
            "ObjectMapprFactory.createObjectMapper() should create ObjectMapper capable of Java 8 Time API deserialization."
        );
        Assertions.assertEquals(
            Month.JULY,
            date.getMonth(),
            "ObjectMapprFactory.createObjectMapper() should create ObjectMapper capable of Java 8 Time API deserialization."
        );
        Assertions.assertEquals(
            2,
            date.getDayOfMonth(),
            "ObjectMapprFactory.createObjectMapper() should create ObjectMapper capable of Java 8 Time API deserialization."
        );
    }

    @Test
    public void handleUnknownProperties() throws IOException {
        var objectMapper = ObjectMapperFactory.createObjectMapper();

        var pojo = objectMapper.readValue("{\"nonExisting\":12}", ObjectMapperFactory.class);

        // if there is no exception everything is fine
    }
}
