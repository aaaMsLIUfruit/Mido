package com.mido.backend.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mido.backend.user.entity.User;
import com.mido.backend.user.mapper.UserMapper;
import com.mido.backend.user.service.UserService;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Override
    public Optional<User> findByUsername(String username) {
        if (username == null) {
            return Optional.empty();
        }
        LambdaQueryWrapper<User> query = new LambdaQueryWrapper<>();
        query.eq(User::getUsername, username);
        return Optional.ofNullable(this.getOne(query, false));
    }

    @Override
    public Optional<User> findByEmail(String email) {
        if (email == null) {
            return Optional.empty();
        }
        LambdaQueryWrapper<User> query = new LambdaQueryWrapper<>();
        query.eq(User::getEmail, email);
        return Optional.ofNullable(this.getOne(query, false));
    }

    @Override
    public Optional<User> findByIdentifier(String identifier) {
        if (identifier == null) {
            return Optional.empty();
        }
        LambdaQueryWrapper<User> query = new LambdaQueryWrapper<>();
        query.eq(User::getUsername, identifier)
                .or(wrapper -> wrapper.eq(User::getEmail, identifier));
        return Optional.ofNullable(this.getOne(query, false));
    }
}

