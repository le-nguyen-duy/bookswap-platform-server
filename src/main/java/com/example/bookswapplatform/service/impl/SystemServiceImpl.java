package com.example.bookswapplatform.service.impl;

import com.example.bookswapplatform.dto.BaseResponseDTO;
import com.example.bookswapplatform.dto.SystemLogDTO;
import com.example.bookswapplatform.entity.SystemLog.Action;
import com.example.bookswapplatform.entity.SystemLog.Object;
import com.example.bookswapplatform.entity.SystemLog.SystemLog;
import com.example.bookswapplatform.entity.User.User;
import com.example.bookswapplatform.repository.SystemLogRepository;
import com.example.bookswapplatform.service.SystemLogService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SystemServiceImpl implements SystemLogService {
    private final ModelMapper modelMapper;
    private final SystemLogRepository systemLogRepository;

    @Override
    public void saveSystemLog(User user, Object object, Action action) {
        SystemLog systemLog = new SystemLog();
        systemLog.setUser(user);
        systemLog.setAction(action);
        systemLog.setObject(object);
        systemLogRepository.save(systemLog);
    }

    @Override
    public ResponseEntity<BaseResponseDTO> getAllSystemLog() {
        List<SystemLog> systemLogs = systemLogRepository.findAll();
        List<SystemLogDTO> systemLogDTOS = new ArrayList<>();
        for (SystemLog systemLog: systemLogs
             ) {
            systemLogDTOS.add(convertToDTO(systemLog));
        }
        return ResponseEntity.ok(new BaseResponseDTO(LocalDateTime.now(), HttpStatus.OK, "Success",null, systemLogDTOS));
    }

    public SystemLogDTO convertToDTO(SystemLog systemLog) {
        if (systemLog == null) {
            return null;
        }
        SystemLogDTO systemLogDTO = new SystemLogDTO();
        systemLogDTO.setId(systemLog.getId());
        systemLogDTO.setAction(systemLog.getAction().toString());
        systemLogDTO.setObject(systemLog.getObject().toString());
        systemLogDTO.setActionTime(systemLog.getActionTime());
        return systemLogDTO;

    }
}
