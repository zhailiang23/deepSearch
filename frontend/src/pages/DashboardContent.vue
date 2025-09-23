<template>
  <div class="grid gap-4 md:gap-8">
    <!-- 统计卡片 -->
    <div class="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
      <div class="rounded-lg border bg-card text-card-foreground shadow-sm">
        <div class="p-6 flex flex-row items-center justify-between space-y-0 pb-2">
          <h3 class="tracking-tight text-sm font-medium">总收入</h3>
          <svg class="h-4 w-4 text-muted-foreground" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 2v20m9-9H3"></path>
          </svg>
        </div>
        <div class="p-6 pt-0">
          <div class="text-2xl font-bold">¥45,231.89</div>
          <p class="text-xs text-muted-foreground">+20.1% 较上月</p>
        </div>
      </div>
      <div class="rounded-lg border bg-card text-card-foreground shadow-sm">
        <div class="p-6 flex flex-row items-center justify-between space-y-0 pb-2">
          <h3 class="tracking-tight text-sm font-medium">新客户</h3>
          <svg class="h-4 w-4 text-muted-foreground" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2"></path>
            <circle cx="8.5" cy="7" r="4"></circle>
            <path d="M20 8v6M23 11h-6"></path>
          </svg>
        </div>
        <div class="p-6 pt-0">
          <div class="text-2xl font-bold">+2350</div>
          <p class="text-xs text-muted-foreground">+180.1% 较上月</p>
        </div>
      </div>
      <div class="rounded-lg border bg-card text-card-foreground shadow-sm">
        <div class="p-6 flex flex-row items-center justify-between space-y-0 pb-2">
          <h3 class="tracking-tight text-sm font-medium">活跃账户</h3>
          <svg class="h-4 w-4 text-muted-foreground" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z"></path>
          </svg>
        </div>
        <div class="p-6 pt-0">
          <div class="text-2xl font-bold">+12,234</div>
          <p class="text-xs text-muted-foreground">+19% 较上月</p>
        </div>
      </div>
      <div class="rounded-lg border bg-card text-card-foreground shadow-sm">
        <div class="p-6 flex flex-row items-center justify-between space-y-0 pb-2">
          <h3 class="tracking-tight text-sm font-medium">增长率</h3>
          <svg class="h-4 w-4 text-muted-foreground" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 7h8m0 0v8m0-8l-8 8-4-4-6 6"></path>
          </svg>
        </div>
        <div class="p-6 pt-0">
          <div class="text-2xl font-bold">+573</div>
          <p class="text-xs text-muted-foreground">+201 较上月</p>
        </div>
      </div>
    </div>

    <!-- 图表和项目区域 -->
    <div class="grid gap-4 md:gap-8 lg:grid-cols-2 xl:grid-cols-3">
      <!-- 概览图表 -->
      <div class="xl:col-span-2 rounded-lg border bg-card text-card-foreground shadow-sm">
        <div class="p-6">
          <h3 class="text-lg font-medium">概览</h3>
        </div>
        <div class="p-6 pt-0">
          <div class="aspect-[4/3] bg-muted rounded-md flex items-center justify-center">
            <div class="text-center">
              <div class="text-2xl font-bold text-muted-foreground mb-2">图表区域</div>
              <p class="text-sm text-muted-foreground">在这里显示数据可视化</p>
            </div>
          </div>
        </div>
      </div>

      <!-- 最近项目 -->
      <div class="rounded-lg border bg-card text-card-foreground shadow-sm">
        <div class="p-6">
          <h3 class="text-lg font-medium">最近项目</h3>
          <p class="text-sm text-muted-foreground">您最近查看的项目概览</p>
        </div>
        <div class="p-6 pt-0">
          <div class="space-y-8">
            <div class="flex items-center">
              <div class="ml-4 space-y-1">
                <p class="text-sm font-medium leading-none">项目 Alpha</p>
                <p class="text-sm text-muted-foreground">进行中</p>
              </div>
              <div class="ml-auto font-medium">+¥1,999.00</div>
            </div>
            <div class="flex items-center">
              <div class="ml-4 space-y-1">
                <p class="text-sm font-medium leading-none">项目 Beta</p>
                <p class="text-sm text-muted-foreground">已完成</p>
              </div>
              <div class="ml-auto font-medium">+¥39.00</div>
            </div>
            <div class="flex items-center">
              <div class="ml-4 space-y-1">
                <p class="text-sm font-medium leading-none">项目 Gamma</p>
                <p class="text-sm text-muted-foreground">计划中</p>
              </div>
              <div class="ml-auto font-medium">+¥299.00</div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 项目详情表格 -->
    <div class="rounded-lg border bg-card text-card-foreground shadow-sm">
      <div class="p-6">
        <h3 class="text-lg font-medium">项目详情</h3>
        <p class="text-sm text-muted-foreground">所有项目的详细信息</p>
      </div>
      <div class="p-6 pt-0">
        <div class="relative w-full overflow-auto">
          <table class="w-full caption-bottom text-sm">
            <thead class="[&_tr]:border-b">
              <tr class="border-b transition-colors hover:bg-muted/50 data-[state=selected]:bg-muted">
                <th class="h-12 px-4 text-left align-middle font-medium text-muted-foreground">项目名称</th>
                <th class="h-12 px-4 text-left align-middle font-medium text-muted-foreground">状态</th>
                <th class="h-12 px-4 text-left align-middle font-medium text-muted-foreground">创建时间</th>
                <th class="h-12 px-4 text-left align-middle font-medium text-muted-foreground">预算</th>
              </tr>
            </thead>
            <tbody class="[&_tr:last-child]:border-0">
              <tr class="border-b transition-colors hover:bg-muted/50 data-[state=selected]:bg-muted">
                <td class="p-4 align-middle">项目 Alpha</td>
                <td class="p-4 align-middle">进行中</td>
                <td class="p-4 align-middle">2024-01-15</td>
                <td class="p-4 align-middle">¥25,000</td>
              </tr>
              <tr class="border-b transition-colors hover:bg-muted/50 data-[state=selected]:bg-muted">
                <td class="p-4 align-middle">项目 Beta</td>
                <td class="p-4 align-middle">已完成</td>
                <td class="p-4 align-middle">2024-01-10</td>
                <td class="p-4 align-middle">¥15,000</td>
              </tr>
              <tr class="border-b transition-colors hover:bg-muted/50 data-[state=selected]:bg-muted">
                <td class="p-4 align-middle">项目 Gamma</td>
                <td class="p-4 align-middle">计划中</td>
                <td class="p-4 align-middle">2024-01-20</td>
                <td class="p-4 align-middle">¥30,000</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
// 仪表板内容组件
</script>