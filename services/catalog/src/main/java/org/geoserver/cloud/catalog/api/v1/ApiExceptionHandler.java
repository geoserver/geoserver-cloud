/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.api.v1;

import java.util.NoSuchElementException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<String> exceptions(ResponseStatusException e) {
        return ResponseEntity.status(e.getStatus())
                .header("Content-Type", "application/xml") //
                .build();
        // .header("Content-Type", "text/plain") //
        // .body(e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> illegalArgumentException(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .header("Content-Type", "application/xml") //
                .build();
        // .header("Content-Type", "text/plain") //
        // .body(e.getMessage());
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<String> noSuchElementException(NoSuchElementException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .header("Content-Type", "application/xml") //
                .build();
        // .header("Content-Type", "text/plain") //
        // .body(e.getMessage());
    }

    // private String body(ResponseStatusException source) {
    // StringWriter stream = new StringWriter();
    // try {
    // XMLStreamWriter writer =
    // XMLOutputFactory.newFactory().createXMLStreamWriter(stream);
    // writer.writeStartDocument();
    // writer.writeStartElement("error");
    //
    // writer.writeStartElement("status");
    // writer.writeCharacters(source.getStatus().toString());
    // writer.writeEndElement();
    //
    // writer.writeStartElement("message");
    // writer.writeCharacters(source.getReason());
    // writer.writeEndElement();
    //
    // writer.writeEndElement();
    // writer.writeEndDocument();
    // String xml = stream.toString();
    // return xml;
    // } catch (XMLStreamException | FactoryConfigurationError e) {
    // throw new RuntimeException(e);
    // }
    // }
}
