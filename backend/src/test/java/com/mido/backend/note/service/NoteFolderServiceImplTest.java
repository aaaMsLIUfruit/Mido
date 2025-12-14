package com.mido.backend.note.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.mido.backend.note.entity.NoteFolder;
import com.mido.backend.note.mapper.NoteFolderMapper;
import com.mido.backend.note.mapper.NoteMapper;
import com.mido.backend.note.service.dto.NoteFolderTreeNode;
import com.mido.backend.note.service.impl.NoteFolderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("笔记文件夹服务测试")
class NoteFolderServiceImplTest {

    @Mock
    private NoteFolderMapper noteFolderMapper;

    @Mock
    private NoteMapper noteMapper;

    @InjectMocks
    private NoteFolderServiceImpl noteFolderService;

    private NoteFolder rootFolder;
    private NoteFolder defaultFolder;
    private NoteFolder childFolder;
    private Long userId = 1L;

    @BeforeEach
    void setUp() throws Exception {
        // 设置MyBatis Plus的baseMapper
        Field baseMapperField = com.baomidou.mybatisplus.extension.service.impl.ServiceImpl.class.getDeclaredField("baseMapper");
        baseMapperField.setAccessible(true);
        baseMapperField.set(noteFolderService, noteFolderMapper);
        
        defaultFolder = new NoteFolder();
        defaultFolder.setId(1L);
        defaultFolder.setUserId(userId);
        defaultFolder.setName("未分类");
        defaultFolder.setParentId(null);
        defaultFolder.setIsDeleted(0);
        defaultFolder.setCreatedAt(LocalDateTime.now());

        rootFolder = new NoteFolder();
        rootFolder.setId(2L);
        rootFolder.setUserId(userId);
        rootFolder.setName("我的文件夹");
        rootFolder.setParentId(null);
        rootFolder.setIsDeleted(0);
        rootFolder.setCreatedAt(LocalDateTime.now());

        childFolder = new NoteFolder();
        childFolder.setId(3L);
        childFolder.setUserId(userId);
        childFolder.setName("子文件夹");
        childFolder.setParentId(2L);
        childFolder.setIsDeleted(0);
        childFolder.setCreatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("创建根文件夹（parentId为null）")
    void createFolder_RootFolder_Success() {
        // Given
        NoteFolder newFolder = new NoteFolder();
        newFolder.setUserId(userId);
        newFolder.setName("新文件夹");
        newFolder.setParentId(null);

        // parentId为null时，validateParent不会调用selectOne，所以不需要mock
        when(noteFolderMapper.insert(any(NoteFolder.class))).thenAnswer(invocation -> {
            NoteFolder folder = invocation.getArgument(0);
            folder.setId(4L);
            return 1;
        });

        // When
        NoteFolder result = noteFolderService.createFolder(newFolder);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("新文件夹");
        assertThat(result.getParentId()).isNull();
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getIsDeleted()).isEqualTo(0);
        assertThat(result.getCreatedAt()).isNotNull();

        verify(noteFolderMapper, times(1)).insert(any(NoteFolder.class));
    }

    @Test
    @DisplayName("在父文件夹下创建子文件夹")
    void createFolder_WithParentFolder_Success() {
        // Given
        NoteFolder newFolder = new NoteFolder();
        newFolder.setUserId(userId);
        newFolder.setName("子文件夹");
        newFolder.setParentId(2L);

        when(noteFolderMapper.selectOne(any(LambdaQueryWrapper.class), anyBoolean())).thenReturn(rootFolder);
        when(noteFolderMapper.insert(any(NoteFolder.class))).thenAnswer(invocation -> {
            NoteFolder folder = invocation.getArgument(0);
            folder.setId(4L);
            return 1;
        });

        // When
        NoteFolder result = noteFolderService.createFolder(newFolder);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getParentId()).isEqualTo(2L);
        verify(noteFolderMapper, times(1)).insert(any(NoteFolder.class));
    }

    @Test
    @DisplayName("父文件夹不存在时抛出异常")
    void createFolder_ParentFolderNotExists_ThrowsException() {
        // Given
        NoteFolder newFolder = new NoteFolder();
        newFolder.setUserId(userId);
        newFolder.setName("子文件夹");
        newFolder.setParentId(999L);

        when(noteFolderMapper.selectOne(any(LambdaQueryWrapper.class), anyBoolean())).thenReturn(null);

        // When & Then
        assertThatThrownBy(() -> noteFolderService.createFolder(newFolder))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST)
                .hasMessageContaining("父文件夹不存在");

        verify(noteFolderMapper, never()).insert(any(NoteFolder.class));
    }

    @Test
    @DisplayName("在'未分类'下创建文件夹应失败")
    void createFolder_UnderDefaultFolder_ThrowsException() {
        // Given
        NoteFolder newFolder = new NoteFolder();
        newFolder.setUserId(userId);
        newFolder.setName("子文件夹");
        newFolder.setParentId(1L); // 未分类的ID

        when(noteFolderMapper.selectOne(any(LambdaQueryWrapper.class), anyBoolean())).thenReturn(defaultFolder);

        // When & Then
        assertThatThrownBy(() -> noteFolderService.createFolder(newFolder))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST)
                .hasMessageContaining("默认文件夹下不能创建子文件夹");

        verify(noteFolderMapper, never()).insert(any(NoteFolder.class));
    }

    @Test
    @DisplayName("获取文件夹树正确构建")
    @org.junit.jupiter.api.Disabled("需要真实数据库，使用集成测试")
    void getFolderTree_BuildsCorrectTree() {
        // 注意：此测试需要真实的Mapper Proxy，已移至集成测试
        // 参见 NoteFolderServiceImplIntegrationTest
    }

    @Test
    @DisplayName("软删除的文件夹不显示在树中")
    @org.junit.jupiter.api.Disabled("需要真实数据库，使用集成测试")
    void getFolderTree_DeletedFoldersNotShown() {
        // 注意：此测试需要真实的Mapper Proxy，已移至集成测试
        // 参见 NoteFolderServiceImplIntegrationTest
    }

    @Test
    @DisplayName("重命名文件夹成功")
    void renameFolder_Success() {
        // Given
        when(noteFolderMapper.selectOne(any(LambdaQueryWrapper.class), anyBoolean())).thenReturn(rootFolder);
        when(noteFolderMapper.updateById(any(NoteFolder.class))).thenReturn(1);

        // When
        noteFolderService.renameFolder(2L, userId, "重命名文件夹");

        // Then
        ArgumentCaptor<NoteFolder> folderCaptor = ArgumentCaptor.forClass(NoteFolder.class);
        verify(noteFolderMapper, times(1)).updateById(folderCaptor.capture());
        assertThat(folderCaptor.getValue().getName()).isEqualTo("重命名文件夹");
    }

    @Test
    @DisplayName("重命名不存在的文件夹抛出异常")
    void renameFolder_NotExists_ThrowsException() {
        // Given
        when(noteFolderMapper.selectOne(any(LambdaQueryWrapper.class), anyBoolean())).thenReturn(null);

        // When & Then
        assertThatThrownBy(() -> noteFolderService.renameFolder(999L, userId, "新名称"))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND)
                .hasMessageContaining("文件夹不存在");

        verify(noteFolderMapper, never()).updateById(any(NoteFolder.class));
    }

    @Test
    @DisplayName("重命名其他用户的文件夹抛出异常")
    void renameFolder_OtherUserFolder_ThrowsException() {
        // Given
        when(noteFolderMapper.selectOne(any(LambdaQueryWrapper.class), anyBoolean())).thenReturn(null);

        // When & Then
        assertThatThrownBy(() -> noteFolderService.renameFolder(2L, 999L, "新名称"))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND);

        verify(noteFolderMapper, never()).updateById(any(NoteFolder.class));
    }

    @Test
    @DisplayName("移动文件夹成功")
    @org.junit.jupiter.api.Disabled("需要真实数据库，使用集成测试")
    void moveFolder_Success() {
        // 注意：此测试需要真实的Mapper Proxy，已移至集成测试
        // 参见 NoteFolderServiceImplIntegrationTest
    }

    @Test
    @DisplayName("不能移动到子文件夹")
    @org.junit.jupiter.api.Disabled("需要真实数据库，lambdaQuery()方法需要Mapper Proxy")
    void moveFolder_CannotMoveToDescendant_ThrowsException() {
        // 注意：此测试需要lambdaQuery().list()来获取所有文件夹，在单元测试中无法Mock
        // 需要使用集成测试配合H2内存数据库
    }

    @Test
    @DisplayName("不能移动到'未分类'文件夹")
    @org.junit.jupiter.api.Disabled("需要真实数据库，lambdaQuery()方法需要Mapper Proxy")
    void moveFolder_CannotMoveToDefaultFolder_ThrowsException() {
        // 注意：此测试需要lambdaQuery().list()来获取所有文件夹，在单元测试中无法Mock
        // 需要使用集成测试配合H2内存数据库
        // Given
        when(noteFolderMapper.selectOne(any(LambdaQueryWrapper.class), anyBoolean()))
                .thenReturn(rootFolder)
                .thenReturn(defaultFolder);

        // When & Then
        assertThatThrownBy(() -> noteFolderService.moveFolder(2L, userId, 1L))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST)
                .hasMessageContaining("默认文件夹下不能创建子文件夹");

        verify(noteFolderMapper, never()).updateById(any(NoteFolder.class));
    }

    @Test
    @DisplayName("删除文件夹成功（软删除）")
    @org.junit.jupiter.api.Disabled("需要真实数据库，lambdaQuery()方法需要Mapper Proxy")
    void deleteFolder_Success() {
        // Given
        when(noteFolderMapper.selectOne(any(LambdaQueryWrapper.class), anyBoolean())).thenReturn(rootFolder);

        List<NoteFolder> allFolders = new ArrayList<>();
        allFolders.add(rootFolder);
        allFolders.add(childFolder);

        when(noteFolderMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(allFolders);
        when(noteFolderMapper.update(any(NoteFolder.class), any(LambdaUpdateWrapper.class))).thenReturn(1);
        when(noteMapper.update(any(), any(LambdaUpdateWrapper.class))).thenReturn(0);

        // When
        noteFolderService.deleteFolder(2L, userId);

        // Then
        // 验证文件夹被软删除
        verify(noteFolderMapper, times(1)).update(any(NoteFolder.class), any(LambdaUpdateWrapper.class));
        // 验证子文件夹也被删除
        verify(noteFolderMapper, times(1)).update(any(NoteFolder.class), any(LambdaUpdateWrapper.class));
    }

    @Test
    @DisplayName("删除不存在的文件夹抛出异常")
    void deleteFolder_NotExists_ThrowsException() {
        // Given
        when(noteFolderMapper.selectOne(any(LambdaQueryWrapper.class), anyBoolean())).thenReturn(null);

        // When & Then
        assertThatThrownBy(() -> noteFolderService.deleteFolder(999L, userId))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND)
                .hasMessageContaining("文件夹不存在");

        verify(noteFolderMapper, never()).update(any(NoteFolder.class), any(LambdaUpdateWrapper.class));
    }
}

