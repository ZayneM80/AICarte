package com.sky.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sky.dto.AddressBookDTO;
import com.sky.entity.AddressBook;

import java.util.List;

public interface AddressBookService extends IService<AddressBook> {

    List<AddressBook> list(AddressBook addressBook);

    void saveDefault(AddressBookDTO addressBookDTO);

    AddressBook getById(Long id);

    void update(AddressBookDTO addressBookDTO);

    void setDefault(AddressBookDTO addressBookDTO);

    void deleteById(Long id);

    AddressBook getDefault();

}
