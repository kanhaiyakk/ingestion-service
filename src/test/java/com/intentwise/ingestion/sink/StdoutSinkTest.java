package com.intentwise.ingestion.sink;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intentwise.ingestion.extract.ExtractedRecord;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

/** Unit tests for {@link StdoutSink}: type, and that write logs every record plus a count. */
class StdoutSinkTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final StdoutSink sink = new StdoutSink();
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();

    @BeforeEach
    void attachAppender() {
        Logger logger = (Logger) LoggerFactory.getLogger(StdoutSink.class);
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void detachAppender() {
        Logger logger = (Logger) LoggerFactory.getLogger(StdoutSink.class);
        logger.detachAppender(appender);
    }

    @Test
    void typeIsStdout() {
        assertThat(sink.type()).isEqualTo("stdout");
    }

    @Test
    void writeLogsEveryRecordAndReportsCount() throws Exception {
        List<ExtractedRecord> records = List.of(
                new ExtractedRecord("bulbasaur", mapper.readTree("{\"name\":\"bulbasaur\"}")),
                new ExtractedRecord("ivysaur", mapper.readTree("{\"name\":\"ivysaur\"}")));

        sink.write(new WriteContext("pokeapi", 7L, records));

        List<String> messages = appender.list.stream().map(ILoggingEvent::getFormattedMessage).toList();
        assertThat(messages).anySatisfy(m -> assertThat(m).contains("bulbasaur"));
        assertThat(messages).anySatisfy(m -> assertThat(m).contains("ivysaur"));
        assertThat(messages).anySatisfy(m -> assertThat(m).contains("2").contains("pokeapi").contains("7"));
    }

    @Test
    void writeTruncatesLongPayloads() throws Exception {
        String longValue = "x".repeat(500);
        ExtractedRecord record = new ExtractedRecord("big", mapper.readTree("{\"value\":\"" + longValue + "\"}"));

        sink.write(new WriteContext("pokeapi", 1L, List.of(record)));

        List<String> messages = appender.list.stream().map(ILoggingEvent::getFormattedMessage).toList();
        assertThat(messages).anySatisfy(m -> assertThat(m).contains("...").doesNotContain(longValue));
    }
}
