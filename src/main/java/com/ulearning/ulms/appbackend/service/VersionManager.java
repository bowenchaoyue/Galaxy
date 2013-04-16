package com.ulearning.ulms.appbackend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ulearning.ulms.appbackend.entity.Platform;
import com.ulearning.ulms.appbackend.entity.Version;
import com.ulearning.ulms.appbackend.repository.mongodb.VersionRepository;
import com.ulearning.ulms.appbackend.util.core.JSONUtils;

/**
 * 版本控制
 * @author heshuai
 * @version 2012-10-26
 *
 * Copyright (c) 2006-2011.Beijing WenHua Online Sci-Tech Development Co. Ltd
 * All rights reserved.
 */
@Service
public class VersionManager {

	@Autowired
	private VersionRepository versionRepository;
	
	public String checkUpdate()
	{
		Version version = versionRepository.findByPlatformOrderByVersionCodeDesc(Platform.ANDROID);
		version.setUrl(Version.UPLOAD_VIRTUAL_PATH + version.getUrl());
		return JSONUtils.toJson(version);
	}
	
	public void saveUpdateRecord(Version version)
	{
		if (version != null ) {
			versionRepository.save(version);
		}
	}
	
}