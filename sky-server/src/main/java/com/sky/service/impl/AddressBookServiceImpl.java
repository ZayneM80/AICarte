package com.sky.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sky.context.BaseContext;
import com.sky.dto.AddressBookDTO;
import com.sky.entity.AddressBook;
import com.sky.mapper.AddressBookMapper;
import com.sky.service.AddressBookService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
public class AddressBookServiceImpl extends ServiceImpl<AddressBookMapper, AddressBook> implements AddressBookService {

    @Override
    public List<AddressBook> list(AddressBook addressBook) {
        Long userId = BaseContext.getCurrentId();

        LambdaQueryWrapper<AddressBook> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AddressBook::getUserId, userId);

        if (addressBook != null) {
            if (addressBook.getPhone() != null && !addressBook.getPhone().isEmpty()) {
                wrapper.like(AddressBook::getPhone, addressBook.getPhone());
            }
            if (addressBook.getConsignee() != null && !addressBook.getConsignee().isEmpty()) {
                wrapper.like(AddressBook::getConsignee, addressBook.getConsignee());
            }
        }

        wrapper.orderByDesc(AddressBook::getId);

        return this.list(wrapper);
    }

    @Override
    @Transactional
    public void saveDefault(AddressBookDTO addressBookDTO) {
        log.info("新增默认地址：{}", addressBookDTO);

        AddressBook addressBook = new AddressBook();
        BeanUtils.copyProperties(addressBookDTO, addressBook);

        Long userId = BaseContext.getCurrentId();
        addressBook.setUserId(userId);
        addressBook.setIsDefault(1);

        this.baseMapper.updateIsDefaultByUserId(userId);

        this.save(addressBook);
    }

    @Override
    public AddressBook getById(Long id) {
        Long userId = BaseContext.getCurrentId();

        LambdaQueryWrapper<AddressBook> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AddressBook::getId, id);
        wrapper.eq(AddressBook::getUserId, userId);

        return this.getOne(wrapper);
    }

    @Override
    @Transactional
    public void update(AddressBookDTO addressBookDTO) {
        log.info("修改地址：{}", addressBookDTO);

        AddressBook addressBook = new AddressBook();
        BeanUtils.copyProperties(addressBookDTO, addressBook);

        if (addressBook.getIsDefault() != null && addressBook.getIsDefault() == 1) {
            Long userId = BaseContext.getCurrentId();
            this.baseMapper.updateIsDefaultByUserId(userId);
        }

        this.updateById(addressBook);
    }

    @Override
    @Transactional
    public void setDefault(AddressBookDTO addressBookDTO) {
        log.info("设置默认地址：{}", addressBookDTO);

        Long userId = BaseContext.getCurrentId();

        this.baseMapper.updateIsDefaultByUserId(userId);

        AddressBook addressBook = new AddressBook();
        addressBook.setId(addressBookDTO.getId());
        addressBook.setIsDefault(1);

        this.updateById(addressBook);
    }

    @Override
    public void deleteById(Long id) {
        Long userId = BaseContext.getCurrentId();

        LambdaQueryWrapper<AddressBook> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AddressBook::getId, id);
        wrapper.eq(AddressBook::getUserId, userId);

        this.remove(wrapper);
    }

    @Override
    public AddressBook getDefault() {
        Long userId = BaseContext.getCurrentId();

        LambdaQueryWrapper<AddressBook> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AddressBook::getUserId, userId);
        wrapper.eq(AddressBook::getIsDefault, 1);

        return this.getOne(wrapper);
    }
}
