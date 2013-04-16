package com.ulearning.ulms.appbackend.service;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ulearning.ulms.appbackend.entity.Apns;
import com.ulearning.ulms.appbackend.entity.Classes;
import com.ulearning.ulms.appbackend.entity.DeviceToken;
import com.ulearning.ulms.appbackend.entity.User;
import com.ulearning.ulms.appbackend.entity.UserCourse;
import com.ulearning.ulms.appbackend.repository.jpa.DeviceTokenRepository;
import com.ulearning.ulms.appbackend.repository.jpa.UserCourseRepository;
import com.ulearning.ulms.appbackend.repository.jpa.UserRepository;
import com.ulearning.ulms.appbackend.repository.mongodb.ApnsRepository;
import com.ulearning.ulms.appbackend.service.apns.IPush;
import com.ulearning.ulms.appbackend.util.core.JSONUtils;
import com.ulearning.ulms.appbackend.util.core.ObjectUtils;
import com.ulearning.ulms.appbackend.util.log.LogUtils;

/**
 * 类说明:消息推送
 * @author heshuai
 * @version 2012-5-7
 *
 * Copyright (c) 2006-2011.Beijing WenHua Online Sci-Tech Development Co. Ltd
 * All rights reserved.
 */
@Service
public class ApnsManager {
	
	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private DeviceTokenRepository deviceTokenRepository;
	
	@Autowired
	private UserCourseRepository userCourseRepository;
	
	@Autowired
	private ApnsRepository apnsRepository;
	
	/**
	 * 
	  * 方法描述：获取该用户的属性标签
	  * @param userID 用户ID
	  * @return 用户属性标签集合
	  * @author heshuai
	  * @version 2013-4-2 下午02:56:43
	 */
	public String getTags(int userID){
		User user = userRepository.findOne(userID);
		if (user != null) {
			Set<String> tags = new LinkedHashSet<String>();
			int orgID = user.getOrganization().getId();
			tags.add(IPush.ORG_KEY + orgID);
			int aspID = user.getOrganization().getAspID();
			tags.add(IPush.ASP_KEY + aspID);
			List<UserCourse> userCourses = userCourseRepository.findById_UserIDOrderByApplyTimeDesc(userID);
			if (userCourses != null && userCourses.size() > 0) {
				for (Iterator<UserCourse> iterator = userCourses.iterator(); iterator.hasNext();) {
					UserCourse userCourse = (UserCourse) iterator.next();
					tags.add(IPush.COURSE_KEY + userCourse.getId().getCourseID());
				}
			}
			Set<Classes> classes = user.getClasses();
			if (classes != null && classes.size() > 0) {
				for (Iterator<Classes> iterator = classes.iterator(); iterator.hasNext();) {
					Classes classEntity = (Classes) iterator.next();
					tags.add(IPush.CLASS_KEY +classEntity.getId());
				}
			}
			return JSONUtils.toJson(tags, Set.class);
		}
		return null;
	}
	
	/**
	 * 
	  * 方法描述：保存客户端post的deviceToken
	  * @param <code>{"userid":418310,"devicetoken":"test","status":1}</code>
	  * @return boolean
	  * @author heshuai
	  * @version 2012-5-9 上午11:32:03
	 */
	public void add(String temp)
	{
		DeviceToken deviceToken = this.ParsingStr(temp);
		if (deviceToken != null) {
			deviceTokenRepository.save(deviceToken);
		}
	}
	/**
	 * 
	  * 方法描述：更新是否允许推送状态设置 status:0/1
	  * @author heshuai
	  * @version 2012-6-4 下午03:19:26
	 */
	public void update(String temp)
	{
		DeviceToken deviceToken = this.ParsingStr(temp);
		if (deviceToken != null) {
			DeviceToken _deviceToken = deviceTokenRepository.findByDevicetoken(deviceToken.getDevicetoken());
			_deviceToken.setStatus(deviceToken.getStatus());
		}
	}
	/**
	 * 
	  * 方法描述：解析Json字符串获取Apn对象
	  * @author heshuai
	  * @version 2012-10-31 下午01:41:34
	 */
	private DeviceToken ParsingStr(String temp)
	{
		if (ObjectUtils.isBlank(temp)) throw new IllegalArgumentException("参数不能为空");
		try {
			JSONObject jsonObject = new JSONObject(temp);
			int _userID = jsonObject.getInt("userid");
			String _token = jsonObject.getString("devicetoken");
			//只保留字母和数字
			String regEx = "[^a-zA-Z0-9]";
			Pattern pattern = Pattern.compile(regEx);
			Matcher matcher = pattern.matcher(_token);
			_token = matcher.replaceAll("");
			int _status = jsonObject.getInt("status");
			DeviceToken deviceToken = new DeviceToken(userRepository.findOne(_userID), _token, _status);
			return deviceToken;
		} catch (JSONException e) {
			LogUtils.doLog("用户设备令牌JSON字符串格式有误", e, temp);
		}
		return null;
	}
	
	/**
	 * 
	  * 方法描述：按照推送类型，推送对象推送消息
	  * @author heshuai
	  * @version 2012-11-1 下午06:13:47
	 */
	public void pushNotifications(Apns apns)
	{
		IPush iPush = apns.getPushType();
		String[] inactiveTokens = iPush.getInvalidDeviceTokens();
		if (!ObjectUtils.isEmpty(inactiveTokens)) {
			deviceTokenRepository.deleteByDevicetokenIn(inactiveTokens);
		}
		apnsRepository.save(apns);
		iPush.pushNotifications(apns);
	}
}