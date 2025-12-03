package com.mido.backend.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mido.backend.user.entity.User;
import java.util.Optional;

public interface UserService extends IService<User> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Optional<User> findByIdentifier(String identifier);
}

