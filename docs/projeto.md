$env:JAVA_HOME = "C:\Users\paulo\.jdks\jdk25"
$env:Path = "C:\Users\paulo\.jdks\jdk25\bin;" + $env:Path
cd backend\eleicao-api
cmd /c "mvnw.cmd spring-boot:run"

## Artefatos administrativos e auditoria

- `GET /api/eleicoes/{id}/relatorios?formato=CSV|XLSX|PDF`: exporta artefatos individuais da eleição.
- `GET /api/eleicoes/{id}/pacote-auditoria`: exporta um ZIP institucional com:
	- `manifesto-auditoria.json` versionado
	- `timeline-auditoria.json` com eventos completos da eleição
	- relatórios consolidados em CSV, XLSX e PDF
- O pacote de auditoria e os formatos `XLSX`/`PDF` só ficam disponíveis após o encerramento da eleição.
- A timeline administrativa passa a registrar exportações de relatório e de pacote ZIP para rastreabilidade institucional.