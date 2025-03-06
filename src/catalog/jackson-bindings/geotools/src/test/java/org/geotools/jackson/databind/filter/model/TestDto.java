package org.geotools.jackson.databind.filter.model;

import java.util.Date;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
public class TestDto {
    private String s;
    private Integer i;
    private Date d;
}
