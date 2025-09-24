package com.ynet.mgmt.searchspace.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ynet.mgmt.jsonimport.dto.ImportExecuteRequest;
import com.ynet.mgmt.jsonimport.dto.ImportTaskStatus;
import com.ynet.mgmt.jsonimport.service.DataImportService;
import com.ynet.mgmt.searchspace.dto.SearchSpaceDTO;
import com.ynet.mgmt.searchspace.service.FileStorageService;
import com.ynet.mgmt.searchspace.service.FileValidationService;
import com.ynet.mgmt.searchspace.service.SearchSpaceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * SearchSpaceController 导入功能集成测试
 */
@WebMvcTest(SearchSpaceController.class)
class SearchSpaceControllerImportIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SearchSpaceService searchSpaceService;

    @MockBean
    private FileValidationService fileValidationService;

    @MockBean
    private FileStorageService fileStorageService;

    @MockBean
    private DataImportService dataImportService;

    @Test
    void testExecuteImport_Success() throws Exception {
        // Given
        Long searchSpaceId = 1L;
        SearchSpaceDTO searchSpace = new SearchSpaceDTO();
        searchSpace.setId(searchSpaceId);
        searchSpace.setName("Test Space");
        searchSpace.setCode("test_space");

        ImportExecuteRequest request = ImportExecuteRequest.builder()
                .taskId("test-task-123")
                .mode(ImportExecuteRequest.ImportMode.APPEND)
                .batchSize(1000)
                .errorHandling(ImportExecuteRequest.ErrorHandlingStrategy.SKIP_ERROR)
                .enableIndexOptimization(true)
                .build();

        ImportTaskStatus mockStatus = ImportTaskStatus.builder()
                .taskId("test-task-123")
                .searchSpaceId(searchSpaceId)
                .state(com.ynet.mgmt.jsonimport.enums.ImportTaskState.PENDING)
                .statusMessage("任务已提交，正在准备执行...")
                .build();

        when(searchSpaceService.getSearchSpace(searchSpaceId)).thenReturn(searchSpace);
        when(dataImportService.executeImport(eq(searchSpaceId), any(ImportExecuteRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(mockStatus));
        when(dataImportService.getImportStatus("test-task-123")).thenReturn(mockStatus);

        // When & Then
        mockMvc.perform(post("/search-spaces/{id}/import/execute", searchSpaceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.taskId").value("test-task-123"))
                .andExpect(jsonPath("$.data.searchSpaceId").value(searchSpaceId))
                .andExpect(jsonPath("$.data.state").value("pending"));

        verify(searchSpaceService).getSearchSpace(searchSpaceId);
        verify(dataImportService).executeImport(eq(searchSpaceId), any(ImportExecuteRequest.class));
    }

    @Test
    void testExecuteImport_SearchSpaceNotFound() throws Exception {
        // Given
        Long nonExistentId = 999L;
        ImportExecuteRequest request = ImportExecuteRequest.builder()
                .taskId("test-task-123")
                .mode(ImportExecuteRequest.ImportMode.APPEND)
                .batchSize(1000)
                .errorHandling(ImportExecuteRequest.ErrorHandlingStrategy.SKIP_ERROR)
                .build();

        when(searchSpaceService.getSearchSpace(nonExistentId))
                .thenThrow(new RuntimeException("搜索空间不存在"));

        // When & Then
        mockMvc.perform(post("/search-spaces/{id}/import/execute", nonExistentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("搜索空间不存在"));

        verify(searchSpaceService).getSearchSpace(nonExistentId);
        verifyNoInteractions(dataImportService);
    }

    @Test
    void testExecuteImport_InvalidRequest() throws Exception {
        // Given
        Long searchSpaceId = 1L;
        ImportExecuteRequest invalidRequest = ImportExecuteRequest.builder()
                .taskId("") // 无效的空任务ID
                .mode(ImportExecuteRequest.ImportMode.APPEND)
                .batchSize(50) // 小于最小值
                .errorHandling(ImportExecuteRequest.ErrorHandlingStrategy.SKIP_ERROR)
                .build();

        // When & Then
        mockMvc.perform(post("/search-spaces/{id}/import/execute", searchSpaceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest()); // 应该因为@Valid验证失败

        verifyNoInteractions(searchSpaceService);
        verifyNoInteractions(dataImportService);
    }

    @Test
    void testGetImportStatus_Success() throws Exception {
        // Given
        Long searchSpaceId = 1L;
        String taskId = "test-task-123";

        ImportTaskStatus mockStatus = ImportTaskStatus.builder()
                .taskId(taskId)
                .searchSpaceId(searchSpaceId)
                .state(com.ynet.mgmt.jsonimport.enums.ImportTaskState.PROCESSING_DATA)
                .totalRecords(1000)
                .processedRecords(500)
                .progressPercentage(50.0)
                .statusMessage("正在处理数据...")
                .build();

        when(dataImportService.getImportStatus(taskId)).thenReturn(mockStatus);

        // When & Then
        mockMvc.perform(get("/search-spaces/{id}/import/status/{taskId}", searchSpaceId, taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.taskId").value(taskId))
                .andExpect(jsonPath("$.data.searchSpaceId").value(searchSpaceId))
                .andExpect(jsonPath("$.data.state").value("processing_data"))
                .andExpect(jsonPath("$.data.progressPercentage").value(50.0));

        verify(dataImportService).getImportStatus(taskId);
    }

    @Test
    void testGetImportStatus_TaskNotFound() throws Exception {
        // Given
        Long searchSpaceId = 1L;
        String taskId = "non-existent-task";

        when(dataImportService.getImportStatus(taskId)).thenReturn(null);

        // When & Then
        mockMvc.perform(get("/search-spaces/{id}/import/status/{taskId}", searchSpaceId, taskId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("任务不存在"));

        verify(dataImportService).getImportStatus(taskId);
    }

    @Test
    void testGetImportStatus_TaskBelongsToWrongSearchSpace() throws Exception {
        // Given
        Long requestedSearchSpaceId = 1L;
        Long actualSearchSpaceId = 2L;
        String taskId = "test-task-123";

        ImportTaskStatus mockStatus = ImportTaskStatus.builder()
                .taskId(taskId)
                .searchSpaceId(actualSearchSpaceId) // 不同的搜索空间ID
                .state(com.ynet.mgmt.jsonimport.enums.ImportTaskState.RUNNING)
                .build();

        when(dataImportService.getImportStatus(taskId)).thenReturn(mockStatus);

        // When & Then
        mockMvc.perform(get("/search-spaces/{id}/import/status/{taskId}", requestedSearchSpaceId, taskId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("任务不存在"));

        verify(dataImportService).getImportStatus(taskId);
    }

    @Test
    void testCancelImport_Success() throws Exception {
        // Given
        Long searchSpaceId = 1L;
        String taskId = "test-task-123";

        ImportTaskStatus mockStatus = ImportTaskStatus.builder()
                .taskId(taskId)
                .searchSpaceId(searchSpaceId)
                .state(com.ynet.mgmt.jsonimport.enums.ImportTaskState.RUNNING)
                .build();

        when(dataImportService.getImportStatus(taskId)).thenReturn(mockStatus);
        when(dataImportService.cancelImport(taskId)).thenReturn(true);

        // When & Then
        mockMvc.perform(delete("/search-spaces/{id}/import/cancel/{taskId}", searchSpaceId, taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("任务已取消"));

        verify(dataImportService).getImportStatus(taskId);
        verify(dataImportService).cancelImport(taskId);
    }

    @Test
    void testCancelImport_TaskCannotBeCancelled() throws Exception {
        // Given
        Long searchSpaceId = 1L;
        String taskId = "test-task-123";

        ImportTaskStatus mockStatus = ImportTaskStatus.builder()
                .taskId(taskId)
                .searchSpaceId(searchSpaceId)
                .state(com.ynet.mgmt.jsonimport.enums.ImportTaskState.COMPLETED) // 已完成，不能取消
                .build();

        when(dataImportService.getImportStatus(taskId)).thenReturn(mockStatus);
        when(dataImportService.cancelImport(taskId)).thenReturn(false);

        // When & Then
        mockMvc.perform(delete("/search-spaces/{id}/import/cancel/{taskId}", searchSpaceId, taskId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("任务当前状态不允许取消"));

        verify(dataImportService).getImportStatus(taskId);
        verify(dataImportService).cancelImport(taskId);
    }
}