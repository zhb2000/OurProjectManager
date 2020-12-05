package com.ourprojmgr.demo.service.impl;

import com.ourprojmgr.demo.dao.IProjectDao;
import com.ourprojmgr.demo.dao.IUserDao;
import com.ourprojmgr.demo.dbmodel.*;
import com.ourprojmgr.demo.jsonmodel.*;
import com.ourprojmgr.demo.service.IProjectService;
import com.ourprojmgr.demo.service.IUserService;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * IProjectService 的实现类
 *
 * @author 谢可睿
 */
@Service
public class ProjectServiceImpl implements IProjectService {
    @Autowired
    IProjectDao projectDao;

    @Autowired
    IUserDao userDao;

    @Autowired
    IUserService userService;

    @Override
    public Project getProjectById(int id) {
        return projectDao.getProjectById(id);
    }

    @Override
    public ProjectJson projectToJson(Project project) {
        if(project == null) return null;
        ProjectJson json = new ProjectJson();
        List<User> admins = userDao.getAdminsByProjectId(project.getId());
        List<UserJson> adminJson = new ArrayList<UserJson>();
        for (User admin: admins) {
            adminJson.add(userService.userToJson(admin));
        }
        json.setAdmins(adminJson);
        json.setCreateAt(project.getCreateAt());
        json.setDescription(project.getDescription());
        json.setId(project.getId());
        json.setName(project.getName());
        User superAdmin = userDao.getSuperAdminByProjectId(project.getId());
        UserJson superAdminJson = userService.userToJson(superAdmin);
        json.setSuperAdmin(superAdminJson);
        json.setUpdateAt(project.getUpdateAt());
        return json;
    }

    @Override
    public Project createProject(Project project) {
        return projectDao.insertProject(project);
    }

    @Override
    public void updateProject(Project project) {
        projectDao.updateProject(project);
    }

    @Override
    public void deleteProject(int projectId) {
        projectDao.deleteProject(projectId);
    }

    @Override
    public boolean isSuperAdminOf(User user, Project project) {
        return
                projectDao.getMemberCount(user.getId(), project.getId(), "SuperAdmin")!= 0;
    }

    @Override
    public boolean isAdminOf(User user, Project project) {
        return projectDao.getMemberCount(user.getId(), project.getId(), "Admin")  +
                projectDao.getMemberCount(user.getId(), project.getId(), "SuperAdmin")!= 0;
    }

    @Override
    public boolean isMemberOf(User user, Project project) {
        return projectDao.getMemberCount(user.getId(), project.getId()) != 0;
    }

    @Override
    public List<User> getMembers(Project project){
        return projectDao.getAllMembers(project.getId());
    }

    @Override
    public void addMember(User user, Project project, String role) {
        projectDao.insertMember(user.getId(), project.getId(), role, LocalDateTime.now());
    }

    @Override
    public void deleteMember(User user, Project project) {
        projectDao.deleteMember(user.getId(), project.getId());
    }

    @Override
    public Invitation getInvitationById(int id) {
        return projectDao.getInvitationById(id);
    }

    @Override
    public InvitationJson invitationToJson(Invitation invitation) {
        if(invitation == null) return null;
        InvitationJson json = new InvitationJson();
        json.setCreateAt(invitation.getCreateAt());
        json.setEndAt(invitation.getEndAt());
        json.setId(invitation.getId());
        Project project = getProjectById(invitation.getProjectId());
        ProjectJson projectJson = projectToJson(project);
        json.setProject(projectJson);
        User receiver = userDao.getUserById(invitation.getReceiverId());
        json.setReceiver(userService.userToJson(receiver));
        User sender = userDao.getUserById(invitation.getSenderId());
        json.setSender(userService.userToJson(sender));
        json.setStatus(invitation.getStatus());
        return json;
    }

    @Override
    public Invitation sendInvitation(User sender, User Receiver, Project project) {
        Invitation invitation = new Invitation();
        invitation.setSenderId(sender.getId());
        invitation.setReceiverId(Receiver.getId());
        invitation.setProjectId(project.getId());
        invitation.setCreateAt(LocalDateTime.now());
        invitation.setStatus(Invitation.STATUS_CREATED);
        projectDao.insertInvitation(invitation);
        return invitation;
    }

    @Override
    public List<Invitation> getInvitations(Project project) {
        return projectDao.getInvitationByProjectId(project.getId());
    }

    @Override
    public void acceptInvitation(User user, Invitation invitation) {
        Project project = getProjectById(invitation.getProjectId());
        invitation.setStatus(Invitation.STATUS_ACCEPTED);
        projectDao.updateInvitation(invitation);
        projectDao.insertMember(user.getId(), invitation.getProjectId(), "Member", LocalDateTime.now());
    }

    @Override
    public void rejectInvitation(User user, Invitation invitation) {
        Project project = getProjectById(invitation.getProjectId());
        invitation.setStatus(Invitation.STATUS_REJECTED);
        projectDao.updateInvitation(invitation);
    }

    @Override
    public void cancelInvitation(User admin, Invitation invitation) {
        Project project = getProjectById(invitation.getProjectId());
        invitation.setStatus(Invitation.STATUS_CANCELED);
        projectDao.updateInvitation(invitation);
    }

    @Override
    public List<CommentJson> getTaskCommentJsons(int taskId) {
        List<Comment> comments = projectDao.getTaskComment(taskId);
        List<CommentJson> commentJsons = new ArrayList<CommentJson>();
        for(Comment comment:comments){
            commentJsons.add(commentToJson(comment));
        }
        return commentJsons;
    }

    @Override
    public Task getTaskById(int taskId, int projectId) {
        return projectDao.getTaskById(taskId, projectId);
    }

    @Override
    public List<Task> getProjectTasks(int projectId){
        return projectDao.getProjectTask(projectId);
    }

    @Override
    public List<User> getExecutors(int taskId){
        return projectDao.getExecutors(taskId);
    }

    @Override
    public CommentJson getTaskCommentJson(int taskId, int commentId) {
        Comment comment = projectDao.getCommentById(commentId, taskId);
        return commentToJson(comment);
    }

    @Override
    public TaskJson taskToJson(Task task){
        TaskJson json = new TaskJson();
        json.setBody(task.getBody());
        json.setCommentNum(projectDao.getCommentCount(task.getId()));
        json.setComplete(task.isComplete());
        json.setCompleteAt(task.getCompleteAt());
        User completer = userDao.getUserById(task.getCompleterId());
        json.setCompleter(userService.userToJson(completer));
        json.setCreateAt(task.getCreateAt());
        User creator = userDao.getUserById(task.getCreatorId());
        json.setCreator(userService.userToJson(creator));
        List<User> executors = projectDao.getExecutors(task.getId());
        List<UserJson> executorJsons = new ArrayList<>();
        executors.forEach(u -> executorJsons.add(userService.userToJson(u)));
        json.setExecutors(executorJsons);
        json.setId(task.getId());
        json.setTitle(task.getTitle());
        return json;
    }

    @Override
    public Task createTask(Task task){
        projectDao.insertTask(task);
        return task;
    }

    @Override
    public void addExecutor(int taskId, int executorId){
        projectDao.insertExecutor(taskId, executorId);
    }

    @Override
    public void deleteExecutor(int taskId, int executorId){
        projectDao.deleteExecutor(taskId, executorId);
    }

    @Override
    public CommentJson saveComment(Comment comment) {
        if(projectDao.getCommentById(comment.getId(), comment.getTaskId()) == null){
            projectDao.insertComment(comment);
        } else {
            projectDao.updateComment(comment);
        }
        return commentToJson(comment);
    }

    @Override
    public void deleteComment(int commentId) {
        projectDao.deleteComment(commentId);
    }

    @Override
    public CommentJson commentToJson(Comment comment) {
        if(comment == null){
            return null;
        }
        CommentJson commentJson = new CommentJson();
        commentJson.setId(comment.getId());
        commentJson.setBody(comment.getBody());
        commentJson.setCreateAt(comment.getCreateAt());
        commentJson.setUser(userToJson(userDao.getUserById(comment.getUserId())));
        return commentJson;
    }

    private UserJson userToJson(User user){
        if(user == null){
            return null;
        }
        UserJson userJson = new UserJson();
        userJson.setId(user.getId());
        userJson.setUsername(user.getUsername());
        userJson.setNickname(user.getNickname());
        userJson.setCreateAt(user.getCreateAt());
        userJson.setUpdateAt(user.getUpdateAt());
        userJson.setProjectCount(userDao.countProjectByUserId(user.getId()));
        return userJson;
    }
}
