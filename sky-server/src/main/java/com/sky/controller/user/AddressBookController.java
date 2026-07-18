package com.sky.controller.user;

import com.sky.dto.AddressBookDTO;
import com.sky.entity.AddressBook;
import com.sky.result.Result;
import com.sky.service.AddressBookService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController("userAddressBookController")
@RequestMapping("/user/addressBook")
@Slf4j
public class AddressBookController {

    @Autowired
    private AddressBookService addressBookService;

    @GetMapping("/list")
    public Result<List<AddressBook>> list(AddressBook addressBook) {
        log.info("查询地址列表");
        List<AddressBook> list = addressBookService.list(addressBook);
        return Result.success(list);
    }

    @PostMapping
    public Result save(@RequestBody AddressBookDTO addressBookDTO) {
        log.info("新增地址：{}", addressBookDTO);
        
        if (addressBookDTO.getIsDefault() != null && addressBookDTO.getIsDefault() == 1) {
            addressBookService.saveDefault(addressBookDTO);
        } else {
            AddressBook addressBook = new AddressBook();
            org.springframework.beans.BeanUtils.copyProperties(addressBookDTO, addressBook);
            
            Long userId = com.sky.context.BaseContext.getCurrentId();
            addressBook.setUserId(userId);
            addressBook.setIsDefault(0);
            addressBookService.save(addressBook);
        }

        return Result.success();
    }

    @GetMapping("/{id}")
    public Result<AddressBook> getById(@PathVariable Long id) {
        log.info("根据ID查询地址，id: {}", id);
        AddressBook addressBook = addressBookService.getById(id);
        return Result.success(addressBook);
    }

    @PutMapping
    public Result update(@RequestBody AddressBookDTO addressBookDTO) {
        log.info("修改地址：{}", addressBookDTO);
        addressBookService.update(addressBookDTO);
        return Result.success();
    }

    @PutMapping("/default")
    public Result setDefault(@RequestBody AddressBookDTO addressBookDTO) {
        log.info("设置默认地址：{}", addressBookDTO);
        addressBookService.setDefault(addressBookDTO);
        return Result.success();
    }

    @DeleteMapping
    public Result deleteById(Long id) {
        log.info("删除地址，id: {}", id);
        addressBookService.deleteById(id);
        return Result.success();
    }

    @GetMapping("/default")
    public Result<AddressBook> getDefault() {
        log.info("查询默认地址");
        AddressBook addressBook = addressBookService.getDefault();
        return Result.success(addressBook);
    }

}
