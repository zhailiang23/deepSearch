<template>
  <div class="container mx-auto px-4 py-8">
    <!-- 统计卡片 -->
    <div v-if="statistics" class="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
      <div class="bg-card rounded-lg border border-green-200 p-4">
        <div class="text-sm text-muted-foreground">总用户数</div>
        <div class="text-2xl font-bold text-foreground">{{ statistics.totalUsers }}</div>
      </div>
      <div class="bg-card rounded-lg border border-green-200 p-4">
        <div class="text-sm text-muted-foreground">活跃用户</div>
        <div class="text-2xl font-bold text-green-600">{{ statistics.activeUsers }}</div>
      </div>
      <div class="bg-card rounded-lg border border-green-200 p-4">
        <div class="text-sm text-muted-foreground">锁定用户</div>
        <div class="text-2xl font-bold text-orange-600">{{ statistics.lockedUsers }}</div>
      </div>
    </div>

    <!-- 工具栏 -->
    <div class="flex flex-col sm:flex-row gap-4 items-start sm:items-center justify-between mb-4">
      <div class="flex flex-col sm:flex-row gap-2 flex-1">
        <!-- 搜索框 -->
        <div class="relative flex-1 max-w-md">
          <input
            v-model="searchKeyword"
            type="text"
            placeholder="搜索用户名、邮箱、全名..."
            class="w-full px-4 py-2 border border-green-300 rounded-md bg-background text-foreground focus:outline-none focus:ring-2 focus:ring-green-500"
            @keyup.enter="handleSearch"
          />
        </div>

        <!-- 状态筛选 -->
        <select
          v-model="filterStatus"
          class="px-4 py-2 border border-green-300 rounded-md bg-background text-foreground focus:ring-2 focus:ring-green-500"
          @change="handleFilter"
        >
          <option :value="undefined">全部状态</option>
          <option :value="UserStatus.ACTIVE">活跃</option>
          <option :value="UserStatus.DISABLED">禁用</option>
          <option :value="UserStatus.LOCKED">锁定</option>
          <option :value="UserStatus.PENDING">待处理</option>
        </select>
      </div>

      <!-- 创建按钮 -->
      <button
        @click="handleCreate"
        class="px-4 py-2 bg-green-600 text-white rounded-md hover:bg-green-700 transition-colors"
      >
        <span class="flex items-center gap-2">
          <span>+</span>
          <span>创建用户</span>
        </span>
      </button>
    </div>

    <!-- 表格 -->
    <div class="bg-card rounded-lg border border-green-200 overflow-hidden">
      <div class="overflow-x-auto">
        <table class="w-full">
          <thead class="bg-green-50">
            <tr>
              <th class="px-4 py-3 text-left text-sm font-medium text-muted-foreground">ID</th>
              <th class="px-4 py-3 text-left text-sm font-medium text-muted-foreground">用户名</th>
              <th class="px-4 py-3 text-left text-sm font-medium text-muted-foreground">邮箱</th>
              <th class="px-4 py-3 text-left text-sm font-medium text-muted-foreground">全名</th>
              <th class="px-4 py-3 text-left text-sm font-medium text-muted-foreground">角色</th>
              <th class="px-4 py-3 text-left text-sm font-medium text-muted-foreground">状态</th>
              <th class="px-4 py-3 text-left text-sm font-medium text-muted-foreground">创建时间</th>
              <th class="px-4 py-3 text-right text-sm font-medium text-muted-foreground">操作</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-green-100">
            <tr
              v-for="user in users"
              :key="user.id"
              class="hover:bg-green-50 transition-colors"
            >
              <td class="px-4 py-3 text-sm text-foreground">{{ user.id }}</td>
              <td class="px-4 py-3 text-sm font-medium text-foreground">{{ user.username }}</td>
              <td class="px-4 py-3 text-sm text-foreground">{{ user.email }}</td>
              <td class="px-4 py-3 text-sm text-foreground">{{ user.fullName || '-' }}</td>
              <td class="px-4 py-3 text-sm">
                <span
                  :class="getRoleColor(user.customRoleCode)"
                  class="inline-flex items-center px-2 py-1 rounded-full text-xs font-medium"
                >
                  {{ user.customRoleName }}
                </span>
              </td>
              <td class="px-4 py-3 text-sm">
                <button
                  @click="handleToggleStatus(user)"
                  :class="getStatusColor(user.status)"
                  class="inline-flex items-center px-2 py-1 rounded-full text-xs font-medium hover:opacity-80 transition-opacity"
                >
                  {{ getStatusLabel(user.status) }}
                </button>
              </td>
              <td class="px-4 py-3 text-sm text-muted-foreground">
                {{ formatDate(user.createdAt) }}
              </td>
              <td class="px-4 py-3 text-sm text-right">
                <div class="flex items-center justify-end gap-2">
                  <button
                    @click="handleEdit(user)"
                    class="px-3 py-1 text-xs text-green-600 hover:text-green-700 transition-colors"
                  >
                    编辑
                  </button>
                  <button
                    @click="handleDelete(user)"
                    class="px-3 py-1 text-xs text-red-600 hover:text-red-700 transition-colors"
                  >
                    删除
                  </button>
                </div>
              </td>
            </tr>
            <tr v-if="users.length === 0">
              <td colspan="8" class="px-4 py-8 text-center text-muted-foreground">
                暂无数据
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- 分页 -->
      <div v-if="pagination.totalPages > 1" class="px-4 py-3 border-t border-green-200">
        <div class="flex items-center justify-between">
          <div class="text-sm text-muted-foreground">
            共 {{ pagination.totalElements }} 条记录，第 {{ pagination.page + 1 }} / {{ pagination.totalPages }} 页
          </div>
          <div class="flex gap-2">
            <button
              @click="handlePageChange(pagination.page - 1)"
              :disabled="pagination.first"
              class="px-3 py-1 text-sm border border-green-300 rounded hover:bg-green-50 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              上一页
            </button>
            <button
              @click="handlePageChange(pagination.page + 1)"
              :disabled="pagination.last"
              class="px-3 py-1 text-sm border border-green-300 rounded hover:bg-green-50 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              下一页
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 创建/编辑表单模态框 -->
    <div
      v-if="showForm"
      class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50"
    >
      <div class="bg-background rounded-lg shadow-xl w-full max-w-md">
        <!-- 头部 -->
        <div class="px-6 py-4 border-b border-green-200">
          <div class="flex items-center justify-between">
            <h3 class="text-lg font-medium text-foreground">
              {{ isEdit ? '编辑用户' : '创建用户' }}
            </h3>
            <button
              @click="handleCloseForm"
              class="text-muted-foreground hover:text-foreground transition-colors"
            >
              ×
            </button>
          </div>
        </div>

        <!-- 表单内容 -->
        <form @submit.prevent="handleSubmit" class="px-6 py-4 space-y-4">
          <!-- 用户名 -->
          <div>
            <label for="username" class="block text-sm font-medium text-foreground mb-1">
              用户名 <span class="text-red-500">*</span>
            </label>
            <input
              id="username"
              v-model="(formData as CreateUserRequest).username"
              type="text"
              required
              :disabled="isEdit"
              class="w-full px-3 py-2 border border-green-300 rounded-md bg-background text-foreground focus:outline-none focus:ring-2 focus:ring-green-500 disabled:bg-gray-100 disabled:cursor-not-allowed"
              placeholder="请输入用户名"
            />
          </div>

          <!-- 邮箱 -->
          <div>
            <label for="email" class="block text-sm font-medium text-foreground mb-1">
              邮箱 <span class="text-red-500">*</span>
            </label>
            <input
              id="email"
              v-model="formData.email"
              type="email"
              required
              class="w-full px-3 py-2 border border-green-300 rounded-md bg-background text-foreground focus:outline-none focus:ring-2 focus:ring-green-500"
              placeholder="请输入邮箱"
            />
          </div>

          <!-- 密码(仅创建时) -->
          <div v-if="!isEdit">
            <label for="password" class="block text-sm font-medium text-foreground mb-1">
              密码 <span class="text-red-500">*</span>
            </label>
            <input
              id="password"
              v-model="(formData as CreateUserRequest).password"
              type="password"
              required
              minlength="6"
              class="w-full px-3 py-2 border border-green-300 rounded-md bg-background text-foreground focus:outline-none focus:ring-2 focus:ring-green-500"
              placeholder="请输入密码(至少6位)"
            />
          </div>

          <!-- 全名 -->
          <div>
            <label for="fullName" class="block text-sm font-medium text-foreground mb-1">
              全名
            </label>
            <input
              id="fullName"
              v-model="formData.fullName"
              type="text"
              class="w-full px-3 py-2 border border-green-300 rounded-md bg-background text-foreground focus:outline-none focus:ring-2 focus:ring-green-500"
              placeholder="请输入全名"
            />
          </div>

          <!-- 手机号 -->
          <div>
            <label for="phone" class="block text-sm font-medium text-foreground mb-1">
              手机号
            </label>
            <input
              id="phone"
              v-model="formData.phone"
              type="tel"
              class="w-full px-3 py-2 border border-green-300 rounded-md bg-background text-foreground focus:outline-none focus:ring-2 focus:ring-green-500"
              placeholder="请输入手机号"
            />
          </div>

          <!-- 角色 -->
          <div>
            <label for="customRole" class="block text-sm font-medium text-foreground mb-1">
              角色 <span class="text-red-500">*</span>
            </label>
            <select
              id="customRole"
              v-model="formData.customRoleId"
              required
              class="w-full px-3 py-2 border border-green-300 rounded-md bg-background text-foreground focus:outline-none focus:ring-2 focus:ring-green-500"
            >
              <option :value="undefined" disabled>请选择角色</option>
              <option v-for="role in roles" :key="role.id" :value="role.id">
                {{ role.name }}
              </option>
            </select>
          </div>

          <!-- 状态 -->
          <div>
            <label for="status" class="block text-sm font-medium text-foreground mb-1">
              状态 <span class="text-red-500">*</span>
            </label>
            <select
              id="status"
              v-model="formData.status"
              required
              class="w-full px-3 py-2 border border-green-300 rounded-md bg-background text-foreground focus:outline-none focus:ring-2 focus:ring-green-500"
            >
              <option :value="UserStatus.ACTIVE">活跃</option>
              <option :value="UserStatus.DISABLED">禁用</option>
              <option :value="UserStatus.LOCKED">锁定</option>
              <option :value="UserStatus.PENDING">待处理</option>
            </select>
          </div>

          <!-- 按钮组 -->
          <div class="flex items-center justify-end space-x-3 pt-4">
            <button
              type="button"
              @click="handleCloseForm"
              class="px-4 py-2 text-sm font-medium text-muted-foreground bg-background border border-green-300 rounded-md hover:bg-green-50 transition-colors"
            >
              取消
            </button>
            <button
              type="submit"
              :disabled="submitting"
              class="px-4 py-2 text-sm font-medium text-white bg-green-600 border border-transparent rounded-md hover:bg-green-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {{ submitting ? '提交中...' : (isEdit ? '更新' : '创建') }}
            </button>
          </div>
        </form>
      </div>
    </div>

    <!-- 删除确认对话框 -->
    <div
      v-if="showDeleteConfirm"
      class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50"
    >
      <div class="bg-background rounded-lg shadow-xl w-full max-w-md">
        <div class="p-6">
          <h3 class="text-lg font-medium text-foreground mb-4">删除用户</h3>
          <p class="text-muted-foreground mb-6">
            确定要删除用户「{{ currentUser?.username }}」吗？此操作不可撤销。
          </p>
          <div class="flex items-center justify-end space-x-3">
            <button
              @click="handleCancelDelete"
              class="px-4 py-2 text-sm font-medium text-muted-foreground bg-background border border-green-300 rounded-md hover:bg-green-50"
            >
              取消
            </button>
            <button
              @click="handleConfirmDelete"
              class="px-4 py-2 text-sm font-medium text-white bg-red-600 border border-transparent rounded-md hover:bg-red-700"
            >
              删除
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 通知 -->
    <div
      v-if="notification.show"
      class="fixed top-4 right-4 bg-background border rounded-lg shadow-lg p-4 z-50"
      :class="{
        'border-green-500': notification.type === 'success',
        'border-red-500': notification.type === 'error'
      }"
    >
      <div class="flex items-start">
        <div class="flex-1">
          <p class="text-sm font-medium text-foreground">{{ notification.title }}</p>
          <p class="text-sm text-muted-foreground mt-1">{{ notification.message }}</p>
        </div>
        <button
          @click="hideNotification"
          class="ml-2 text-muted-foreground hover:text-foreground"
        >
          ×
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { UserStatus } from '@/types/user'
import type { User, CreateUserRequest, UpdateUserRequest, UserStatistics } from '@/types/user'
import { roleApi } from '@/services/roleApi'
import type { Role } from '@/types/role'

const store = useUserStore()

// 状态管理
const showForm = ref(false)
const showDeleteConfirm = ref(false)
const currentUser = ref<User | null>(null)
const statistics = ref<UserStatistics | null>(null)
const roles = ref<Role[]>([])

const searchKeyword = ref('')
const filterStatus = ref<UserStatus | undefined>(undefined)
const filterCustomRoleId = ref<number | undefined>(undefined)

// 表单数据
const formData = ref<CreateUserRequest | UpdateUserRequest>({
  username: '',
  email: '',
  password: '',
  fullName: '',
  phone: '',
  status: UserStatus.ACTIVE,
  customRoleId: undefined
})

const submitting = ref(false)
const isEdit = computed(() => currentUser.value !== null)

// 通知状态
const notification = ref({
  show: false,
  type: 'success' as 'success' | 'error' | 'warning' | 'info',
  title: '',
  message: ''
})

// 计算属性
const users = computed(() => store.users)
const pagination = computed(() => store.pagination)

// 工具函数
const formatDate = (date: string): string => {
  return new Date(date).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const getRoleColor = (roleCode: string): string => {
  // 可以根据角色代码定义不同的颜色
  return roleCode === 'ADMIN' ? 'bg-green-100 text-green-800' : 'bg-blue-100 text-blue-800'
}

const getStatusLabel = (status: UserStatus): string => {
  const labels: Record<UserStatus, string> = {
    [UserStatus.ACTIVE]: '活跃',
    [UserStatus.DISABLED]: '禁用',
    [UserStatus.LOCKED]: '锁定',
    [UserStatus.PENDING]: '待处理'
  }
  return labels[status]
}

const getStatusColor = (status: UserStatus): string => {
  const colors: Record<UserStatus, string> = {
    [UserStatus.ACTIVE]: 'bg-green-100 text-green-800',
    [UserStatus.DISABLED]: 'bg-gray-100 text-gray-800',
    [UserStatus.LOCKED]: 'bg-red-100 text-red-800',
    [UserStatus.PENDING]: 'bg-yellow-100 text-yellow-800'
  }
  return colors[status]
}

// 显示通知
const showNotification = (type: typeof notification.value.type, title: string, message: string) => {
  notification.value = {
    show: true,
    type,
    title,
    message
  }

  setTimeout(() => {
    hideNotification()
  }, 3000)
}

// 隐藏通知
const hideNotification = () => {
  notification.value.show = false
}

// 加载统计信息
const loadStatistics = async () => {
  try {
    statistics.value = await store.getStatistics()
  } catch (error) {
    console.error('加载统计信息失败:', error)
  }
}

// 加载角色列表
const loadRoles = async () => {
  try {
    const response = await roleApi.getAll()
    roles.value = response.data || []
  } catch (error) {
    console.error('加载角色列表失败:', error)
  }
}

// 加载数据
const loadData = async () => {
  await store.fetchUsers({
    page: store.queryParams.page,
    size: store.queryParams.size,
    keyword: searchKeyword.value || undefined,
    status: filterStatus.value,
    customRoleId: filterCustomRoleId.value
  })
}

// 事件处理
const handleSearch = () => {
  store.updateQueryParams({ page: 0 })

  if (searchKeyword.value.trim()) {
    store.searchUsers(searchKeyword.value.trim(), {
      page: 0,
      size: store.queryParams.size,
      status: filterStatus.value,
      customRoleId: filterCustomRoleId.value
    })
  } else {
    loadData()
  }
}

const handleFilter = () => {
  store.updateQueryParams({ page: 0 })
  loadData()
}

const handlePageChange = (page: number) => {
  store.updateQueryParams({ page })
  loadData()
}

const handleCreate = () => {
  currentUser.value = null
  formData.value = {
    username: '',
    email: '',
    password: '',
    fullName: '',
    phone: '',
    status: UserStatus.ACTIVE,
    customRoleId: undefined
  }
  showForm.value = true
}

const handleEdit = (user: User) => {
  currentUser.value = user
  formData.value = {
    email: user.email,
    fullName: user.fullName || '',
    phone: user.phone || '',
    status: user.status,
    customRoleId: user.customRoleId
  }
  showForm.value = true
}

const handleDelete = (user: User) => {
  currentUser.value = user
  showDeleteConfirm.value = true
}

const handleCloseForm = () => {
  showForm.value = false
  currentUser.value = null
}

const handleSubmit = async () => {
  try {
    submitting.value = true

    let result: User
    if (isEdit.value && currentUser.value) {
      result = await store.updateUser(currentUser.value.id, formData.value as UpdateUserRequest)
      showNotification('success', '更新成功', `用户「${result.username}」已更新成功`)
    } else {
      result = await store.createUser(formData.value as CreateUserRequest)
      showNotification('success', '创建成功', `用户「${result.username}」已创建成功`)
    }

    showForm.value = false
    currentUser.value = null
    await loadStatistics()
  } catch (error) {
    showNotification(
      'error',
      isEdit.value ? '更新失败' : '创建失败',
      error instanceof Error ? error.message : '操作失败'
    )
  } finally {
    submitting.value = false
  }
}

const handleConfirmDelete = async () => {
  if (!currentUser.value) return

  try {
    const username = currentUser.value.username
    await store.deleteUser(currentUser.value.id)

    showNotification('success', '删除成功', `用户「${username}」已删除成功`)
    await loadStatistics()
  } catch (error) {
    showNotification(
      'error',
      '删除失败',
      error instanceof Error ? error.message : '删除用户失败'
    )
  } finally {
    showDeleteConfirm.value = false
    currentUser.value = null
  }
}

const handleCancelDelete = () => {
  showDeleteConfirm.value = false
  currentUser.value = null
}

const handleToggleStatus = async (user: User) => {
  try {
    await store.toggleStatus(user.id)
    await loadStatistics()
    showNotification('success', '状态切换成功', `用户「${user.username}」状态已更新`)
  } catch (error) {
    showNotification(
      'error',
      '状态切换失败',
      error instanceof Error ? error.message : '切换状态失败'
    )
  }
}

// 初始化
onMounted(async () => {
  await Promise.all([loadStatistics(), loadData(), loadRoles()])
})
</script>

<style scoped>
.container {
  max-width: 1400px;
}
</style>