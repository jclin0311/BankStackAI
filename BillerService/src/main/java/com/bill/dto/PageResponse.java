package com.bill.dto;


import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class PageResponse<T> {
    private List<T> items;
    private long total;
    private int limit;
    private int offset;
}
