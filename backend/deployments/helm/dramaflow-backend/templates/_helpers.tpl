{{- define "dramaflow-backend.name" -}}
dramaflow-backend
{{- end -}}

{{- define "dramaflow-backend.fullname" -}}
{{ printf "%s-%s" .Release.Name (include "dramaflow-backend.name" .) }}
{{- end -}}
