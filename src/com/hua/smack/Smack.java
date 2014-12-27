package com.hua.smack;

import com.hua.exception.IMException;

/** 
 * 首先定义一些接口，需要实现一些什么样的功能， 
 *  
 * @author way 
 *  
 */  
public interface Smack {  
    /** 
     * 登陆 
     *  
     * @param account 
     *            账号 
     * @param password 
     *            密码 
     * @return 是否登陆成功 
     * @throws IMException 
     *             抛出自定义异常，以便统一处理登陆失败的问题 
     */  
    public boolean login(String account, String password) throws IMException;  
  
    /** 
     * 注销登陆 
     *  
     * @return 是否成功 
     */  
    public boolean logout();  
  
    /** 
     * 是否已经连接上服务器 
     *  
     * @return 
     */  
    public boolean isAuthenticated();  
  
    /** 
     * 添加好友 
     *  
     * @param user 
     *            好友id 
     * @param alias 
     *            昵称 
     * @param group 
     *            所在的分组 
     * @throws IMException 
     */  
    public void addRosterItem(String user, String alias, String group)  
            throws IMException;  
  
    /** 
     * 删除好友 
     *  
     * @param user 
     *            好友id 
     * @throws IMException 
     */  
    public void removeRosterItem(String user) throws IMException;  
  
    /** 
     * 修改好友昵称 
     *  
     * @param user 
     *            好友id 
     * @param newName 
     *            新昵称 
     * @throws IMException 
     */  
    public void renameRosterItem(String user, String newName)  
            throws IMException;  
  
    /** 
     * 移动好友到新分组 
     *  
     * @param user 
     *            好友id 
     * @param group 
     *            新组名 
     * @throws IMException 
     */  
    public void moveRosterItemToGroup(String user, String group)  
            throws IMException;  
  
    /** 
     * 重命名分组 
     *  
     * @param group 
     *            之前的组名 
     * @param newGroup 
     *            新组名 
     */  
    public void renameRosterGroup(String group, String newGroup);  
  
    /** 
     * 请求好友重新授权，用在添加好友失败时，重复添加 再次向对方发出申请 
     *  
     * @param user 
     *            好友id 
     */  
    public void requestAuthorizationForRosterItem(String user);  
  
    /** 
     * 添加新分组 
     *  
     * @param group 
     */  
    public void addRosterGroup(String group);  
  
    /** 
     * 设置当前在线状态 
     */  
    public void setStatusFromConfig();  
  
    /** 
     * 发送消息 
     *  
     * @param user 
     * @param message 
     */  
    public void sendMessage(String user, String message);  
  
    /** 
     * 向服务器发送心跳包，保持长连接 通过一个闹钟控制，定时发送， 
     */  
    public void sendServerPing();  
  
    /** 
     * 从jid中获取好友名 
     *  
     * @param jid 
     * @return 
     */  
    public String getNameForJID(String jid);  
}  
/*
public interface Smack {
	public boolean login(String account, String password) throws XXException;

	public boolean logout();

	public boolean isAuthenticated();

	public void addRosterItem(String user, String alias, String group)
			throws XXException;

	public void removeRosterItem(String user) throws XXException;

	public void renameRosterItem(String user, String newName)
			throws XXException;

	public void moveRosterItemToGroup(String user, String group)
			throws XXException;

	public void renameRosterGroup(String group, String newGroup);

	public void requestAuthorizationForRosterItem(String user);

	public void addRosterGroup(String group);

	public void setStatusFromConfig();

	public void sendMessage(String user, String message);

	public void sendServerPing();

	public String getNameForJID(String jid);
}*/
