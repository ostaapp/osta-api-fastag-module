package com.dipcoin.api.model;

import java.util.List;

import lombok.Data;

@Data
public class TollTagsWihVinDetail extends APIResponse {

    private List<TollTagWihVinDetail> tollTagWihVinDetails;

    private Pagination pagination;

}