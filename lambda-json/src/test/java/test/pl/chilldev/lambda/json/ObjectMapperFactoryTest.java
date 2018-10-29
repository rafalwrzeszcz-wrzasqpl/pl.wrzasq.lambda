/*
 * This file is part of the ChillDev-Lambda.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2018 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package test.pl.chilldev.lambda.json;

import java.io.IOException;
import java.time.LocalDate;
import java.time.Month;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;
import pl.chilldev.lambda.json.ObjectMapperFactory;

public class ObjectMapperFactoryTest
{
    @Test
    public void handleJava8TimeApiSerialization() throws IOException
    {
        ObjectMapper objectMapper = ObjectMapperFactory.createObjectMapper();

        LocalDate date = LocalDate.of(2011, Month.JANUARY, 30);

        String json = objectMapper.writeValueAsString(date);

        Assert.assertEquals(
            "ObjectMapprFactory.createObjectMapper() should create ObjectMapper capable of Java 8 Time API serialization.",
            "\"2011-01-30\"",
            json
        );
    }

    @Test
    public void handleJava8TimeApiDeserialization() throws IOException
    {
        ObjectMapper objectMapper = ObjectMapperFactory.createObjectMapper();

        LocalDate date = objectMapper.readValue("\"2015-07-02\"", LocalDate.class);

        Assert.assertEquals(
            "ObjectMapprFactory.createObjectMapper() should create ObjectMapper capable of Java 8 Time API deserialization.",
            2015,
            date.getYear()
        );
        Assert.assertEquals(
            "ObjectMapprFactory.createObjectMapper() should create ObjectMapper capable of Java 8 Time API deserialization.",
            Month.JULY,
            date.getMonth()
        );
        Assert.assertEquals(
            "ObjectMapprFactory.createObjectMapper() should create ObjectMapper capable of Java 8 Time API deserialization.",
            2,
            date.getDayOfMonth()
        );
    }

    @Test
    public void handleUnknownProperties() throws IOException
    {
        ObjectMapper objectMapper = ObjectMapperFactory.createObjectMapper();

        ObjectMapperFactory pojo = objectMapper.readValue("{\"nonExisting\":12}", ObjectMapperFactory.class);

        // if there is no exception everything is fine
    }
}
