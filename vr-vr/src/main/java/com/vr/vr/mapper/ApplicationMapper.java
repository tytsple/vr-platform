package com.vr.vr.mapper;

import com.vr.vr.domain.Application;
import com.vr.vr.domain.AppVersion;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ApplicationMapper {
    @Select("SELECT id, name, description, created_at FROM applications ORDER BY id")
    List<Application> selectApplicationList();

    @Select("SELECT id, name, description, created_at FROM applications WHERE id=#{id}")
    Application selectApplicationById(Long id);

    @Insert("INSERT INTO applications (name, description) VALUES (#{name}, #{description})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertApplication(Application app);

    @Update("UPDATE applications SET name=#{name}, description=#{description} WHERE id=#{id}")
    int updateApplication(Application app);

    @Delete("DELETE FROM applications WHERE id=#{id}")
    int deleteApplicationById(Long id);

    @Select("SELECT id, application_id, version, created_at FROM application_versions WHERE application_id=#{appId} ORDER BY created_at DESC")
    List<AppVersion> selectVersionsByAppId(Long appId);

    @Insert("INSERT INTO application_versions (application_id, version) VALUES (#{applicationId}, #{version})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertVersion(AppVersion v);

    @Delete("DELETE FROM application_versions WHERE id=#{id}")
    int deleteVersionById(Long id);
}
