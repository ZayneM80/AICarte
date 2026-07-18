package com.sky.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class AddressBookDTO implements Serializable {

    private Long id;

    private String consignee;

    private String phone;

    private String sex;

    private String provinceCode;

    private String provinceName;

    private String cityCode;

    private String cityName;

    private String districtCode;

    private String districtName;

    private String detail;

    private String label;

    private Integer isDefault;

}
