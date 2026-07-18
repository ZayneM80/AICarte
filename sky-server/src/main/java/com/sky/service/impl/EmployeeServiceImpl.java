package com.sky.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sky.constant.MessageConstant;
import com.sky.constant.PasswordConstant;
import com.sky.constant.StatusConstant;
import com.sky.context.BaseContext;
import com.sky.dto.EmployeeDTO;
import com.sky.dto.EmployeeLoginDTO;
import com.sky.dto.EmployeePageQueryDTO;
import com.sky.entity.Employee;
import com.sky.exception.AccountLockedException;
import com.sky.exception.AccountNotFoundException;
import com.sky.exception.PasswordErrorException;
import com.sky.mapper.EmployeeMapper;
import com.sky.result.PageResult;
import com.sky.service.EmployeeService;
import com.sky.vo.EmployeeVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.time.LocalDateTime;

@Service
@Slf4j
public class EmployeeServiceImpl extends ServiceImpl<EmployeeMapper, Employee> implements EmployeeService {

    /**
     * 员工登录
     *
     * @param employeeLoginDTO
     * @return
     */
    public Employee login(EmployeeLoginDTO employeeLoginDTO) {
        String username = employeeLoginDTO.getUsername();
        String password = employeeLoginDTO.getPassword();

        Employee employee = this.getOne(
            new LambdaQueryWrapper<Employee>()
                .eq(Employee::getUsername, username)
        );

        if (employee == null) {
            throw new AccountNotFoundException(MessageConstant.ACCOUNT_NOT_FOUND);
        }

        password = DigestUtils.md5DigestAsHex(password.getBytes());
        if (!password.equals(employee.getPassword())) {
            throw new PasswordErrorException(MessageConstant.PASSWORD_ERROR);
        }

        if (employee.getStatus() == StatusConstant.DISABLE) {
            throw new AccountLockedException(MessageConstant.ACCOUNT_LOCKED);
        }

        employee.setPassword(null);
        employee.setCreateUser(null);
        employee.setUpdateUser(null);

        return employee;
    }

    /**
     * 添加员工
     * 将员工信息保存到数据库，默认状态为启用，密码为默认密码的 MD5 值
     *
     * @param employeeDTO 员工数据传输对象，包含用户名、姓名、手机号、性别、身份证号等信息
     */
    public void addEmployee(EmployeeDTO employeeDTO) {
        // 创建员工实体对象
        Employee employee = new Employee();
        
        // 将 DTO 中的属性值拷贝到实体对象
        BeanUtils.copyProperties(employeeDTO, employee);
        
        // 设置员工状态为启用状态
        employee.setStatus(StatusConstant.ENABLE);
        
        // 设置默认密码并进行 MD5 加密
        employee.setPassword(DigestUtils.md5DigestAsHex(PasswordConstant.DEFAULT_PASSWORD.getBytes()));
        
        // 设置创建时间和更新时间为当前时间
        employee.setCreateTime(LocalDateTime.now());
        employee.setUpdateTime(LocalDateTime.now());
        
        // 设置创建人和更新人为当前登录用户
        Long currentId = BaseContext.getCurrentId();
        employee.setCreateUser(currentId);
        employee.setUpdateUser(currentId);
        
        // 调用父类方法保存到数据库
        super.save(employee);
    }

    /**
     * 分页查询员工
     * 根据姓名模糊查询，按创建时间降序排序
     *
     * @param employeePageQueryDTO 分页查询条件，包含页码、每页记录数、姓名
     * @return 分页结果，包含总记录数和当前页数据
     */
    public PageResult pageQuery(EmployeePageQueryDTO employeePageQueryDTO) {
        // 创建 Page 对象，设置页码和每页记录数
        Page<Employee> page = new Page<>(
            employeePageQueryDTO.getPage(), 
            employeePageQueryDTO.getPageSize()
        );
        
        // 创建查询条件
        LambdaQueryWrapper<Employee> wrapper = new LambdaQueryWrapper<>();
        
        // 如果有姓名条件，添加模糊查询
        if (StringUtils.isNotBlank(employeePageQueryDTO.getName())) {
            wrapper.like(Employee::getName, employeePageQueryDTO.getName());
        }
        
        // 按创建时间降序排序
        wrapper.orderByDesc(Employee::getCreateTime);
        
        // 执行分页查询
        Page<Employee> resultPage = this.page(page, wrapper);
        
        // 封装分页结果
        return new PageResult(
            resultPage.getTotal(),  // 总记录数
            resultPage.getRecords() // 当前页数据
        );
    }

    /**
     * 启用或禁用员工账号
     * 根据员工 ID 更新状态，同时更新更新时间和更新人
     *
     * @param id 员工 ID
     * @param status 状态（0-禁用，1-启用）
     */
    public void updateStatus(Long id, Integer status) {
        // 创建更新条件
        LambdaQueryWrapper<Employee> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Employee::getId, id);
        
        // 创建更新实体
        Employee updateEmployee = new Employee();
        updateEmployee.setStatus(status);
        updateEmployee.setUpdateTime(LocalDateTime.now());
        updateEmployee.setUpdateUser(BaseContext.getCurrentId());
        
        // 执行更新
        this.update(updateEmployee, wrapper);
    }

    /**
     * 根据 ID 查询员工信息
     * 将查询结果转换为 EmployeeVO 返回
     *
     * @param id 员工 ID
     * @return 员工信息视图对象
     */
    public EmployeeVO getById(Long id) {
        // 根据 ID 查询员工（调用父类的 getById 方法）
        Employee employee = super.getById(id);
        
        // 将 Employee 转换为 EmployeeVO
        return EmployeeVO.builder()
            .id(employee.getId())
            .username(employee.getUsername())
            .name(employee.getName())
            .phone(employee.getPhone())
            .sex(employee.getSex())
            .idNumber(employee.getIdNumber())
            .status(employee.getStatus())
            .build();
    }

    /**
     * 修改员工信息
     * 更新员工基本信息，同时更新更新时间和更新人
     *
     * @param employeeDTO 员工数据传输对象，包含修改后的员工信息
     */
    public void update(EmployeeDTO employeeDTO) {
        // 创建更新实体
        Employee employee = new Employee();
        
        // 将 DTO 中的属性值拷贝到实体对象
        BeanUtils.copyProperties(employeeDTO, employee);
        
        // 设置更新时间为当前时间
        employee.setUpdateTime(LocalDateTime.now());
        
        // 设置更新人为当前登录用户
        employee.setUpdateUser(BaseContext.getCurrentId());
        
        // 调用父类方法更新
        this.updateById(employee);
    }

}
