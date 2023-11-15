package com.example.bookswapplatform.service.impl;

import com.example.bookswapplatform.dto.AreaDTO;
import com.example.bookswapplatform.dto.BaseResponseDTO;
import com.example.bookswapplatform.dto.DistrictDTO;
import com.example.bookswapplatform.entity.Area.Area;
import com.example.bookswapplatform.entity.Area.District;
import com.example.bookswapplatform.repository.AreaRepository;
import com.example.bookswapplatform.service.AreaService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AreaServiceImpl implements AreaService {
    private final AreaRepository areaRepository;
    private final ModelMapper modelMapper;

    @Override
    public ResponseEntity<BaseResponseDTO> getAllArea() {
        Set<AreaDTO> areaDTOS = new HashSet<>();
        for (Area area: areaRepository.findAll()
             ) {
            areaDTOS.add(convertToDTO(area));
        }
        return ResponseEntity.ok(new BaseResponseDTO(LocalDateTime.now(), HttpStatus.OK, "Successfully", null, areaDTOS));
    }


    public AreaDTO convertToDTO (Area area) {
        Set<DistrictDTO> districtDTOS = new HashSet<>();
        if(area == null) {
            return null;
        }
        AreaDTO areaDTO = modelMapper.map(area, AreaDTO.class);
        for (District district: area.getDistricts()
             ) {
            DistrictDTO districtDTO = modelMapper.map(district, DistrictDTO.class);
            districtDTOS.add(districtDTO);
        }

        areaDTO.setDistrictDTOS(districtDTOS);
        return areaDTO;
    }
}
