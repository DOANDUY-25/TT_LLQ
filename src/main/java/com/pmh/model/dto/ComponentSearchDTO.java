package com.pmh.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ComponentSearchDTO {
    private List<String> componentCodes;
    private List<String> componentNames;
    private List<Integer> statuses;
    private List<Integer> isActives;
    private Integer page = 0;
    private Integer size = 20;
}
