package rbac

import "slices"

const (
	RoleSuperAdmin    = "super_admin"
	RoleOpsManager    = "ops_manager"
	RoleContentEditor = "content_editor"
	RoleSupportAgent  = "support_agent"
	RoleFinanceSupport = "finance_support"
)

const (
	PermDashboardView        = "dashboard:view"
	PermDramaRead            = "drama:read"
	PermDramaWrite           = "drama:write"
	PermEpisodeWrite         = "episode:write"
	PermFeedConfigRead       = "feed_config:read"
	PermFeedConfigWrite      = "feed_config:write"
	PermPurchaseRead         = "purchase:read"
	PermPurchaseResync       = "purchase:resync"
	PermEntitlementRead      = "entitlement:read"
	PermEntitlementRecompute = "entitlement:recompute"
	PermEntitlementGrantTemp = "entitlement:grant_temp"
	PermEntitlementRevoke    = "entitlement:revoke"
	PermRTDNRead             = "rtdn:read"
	PermRTDNReplay           = "rtdn:replay"
	PermPlaybackRead         = "playback:read"
	PermAuditRead            = "audit:read"
	PermSettingsView         = "settings:view"
)

var rolePermissions = map[string][]string{
	RoleSuperAdmin: {
		PermDashboardView, PermDramaRead, PermDramaWrite, PermEpisodeWrite, PermFeedConfigRead, PermFeedConfigWrite,
		PermPurchaseRead, PermPurchaseResync, PermEntitlementRead, PermEntitlementRecompute, PermEntitlementGrantTemp,
		PermEntitlementRevoke, PermRTDNRead, PermRTDNReplay, PermPlaybackRead, PermAuditRead, PermSettingsView,
	},
	RoleOpsManager: {
		PermDashboardView, PermDramaRead, PermDramaWrite, PermEpisodeWrite, PermFeedConfigRead, PermFeedConfigWrite,
		PermPurchaseRead, PermPurchaseResync, PermEntitlementRead, PermEntitlementRecompute, PermRTDNRead, PermRTDNReplay,
		PermPlaybackRead, PermAuditRead, PermSettingsView,
	},
	RoleContentEditor: {
		PermDashboardView, PermDramaRead, PermDramaWrite, PermEpisodeWrite, PermFeedConfigRead, PermFeedConfigWrite, PermSettingsView,
	},
	RoleSupportAgent: {
		PermDashboardView, PermPurchaseRead, PermEntitlementRead, PermEntitlementRecompute, PermRTDNRead, PermPlaybackRead, PermSettingsView,
	},
	RoleFinanceSupport: {
		PermDashboardView, PermPurchaseRead, PermPurchaseResync, PermEntitlementRead, PermRTDNRead, PermAuditRead, PermSettingsView,
	},
}

func Permissions(role string) []string {
	items := rolePermissions[role]
	out := make([]string, len(items))
	copy(out, items)
	return out
}

func Allowed(role string, permission string) bool {
	return slices.Contains(rolePermissions[role], permission)
}
