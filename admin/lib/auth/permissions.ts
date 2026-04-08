export type AdminSession = {
  adminUser: {
    id: string;
    email: string;
    displayName: string;
    role: string;
    status: string;
  };
  permissions: string[];
};

export function can(session: AdminSession | null, permission: string) {
  if (!session) {
    return false;
  }

  // 2026-04-08:
  // 超级管理员权限这里继续保留 "*" 兜底，不在前端把角色名硬编码成一堆 if/else。
  // 这样做的好处是权限模型变更时，只要后端会话里给的 permissions 还稳定，前端不用跟着大改。
  if (session.permissions.includes("*")) {
    return true;
  }

  return session.permissions.includes(permission);
}
