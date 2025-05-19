# Task Scheduler Service del progetto TrasparenzAI
## Task Scheduler Service - Servizi programmati a tempo

[![Supported JVM Versions](https://img.shields.io/badge/JVM-21-brightgreen.svg?style=for-the-badge&logo=Java)](https://openjdk.java.net/install/)

Task Scheduler Service è parte della suite di servizi per la verifica delle informazioni sulla
Trasparenza dei siti web delle Pubbliche amministrazioni italiane.
 
## Task Scheduler Service

Task Scheduler Service è il componente che si occupa di avviare alcuni processi eseguiti a intervalli fissi, come
per esempio l'avvio delle scansioni dei siti del PA per la verifica della corrispondenza dei requisiti.

Nell'utilizzo tramite **docker-compose.yml** ricordarsi di impostare la corretta variabile d'ambiente che specifica
l'url del config-service da utilizzare, impostandola nel file **.env**.

```
  #nel docker-compose.yml
  environment:
    - confighost=${CONFIG_HOST}
```

```
  #nel file .env
  CONFIG_HOST=https://dica33.ba.cnr.it/config-service/config
```

E' importante che le informazioni nel config-service siano corrette per l'ambiente che stiamo utilizzando,
per esempio il parametro *workflow.cron.url* deve contenere l'URL del servizio *conductor* dove
effettuare le operazioni sui workflow (per esempio la cancellazione di quelli vecchi).

Le informazioni di configurazione dei cron relativi ai workflow possono essere visualizzate 
all'url **/tasks/workflowCronConfig.

Il task-scheduler-service è un progetto *spring-boot*, quindi per necessità è possibile sovrascrivere
le proprietà prelevate dal *config-service* impostandole come variabili d'ambiente nel *docker-compose.yml*,
per esempio:

```
  environment:
    - workflow.cron.url=https://dica33.ba.cnr.it/mockserver/conductor-server/api/workflow
    - workflow.cron.expression=08 * ? * MON-FRI
```

# <img src="https://www.docker.com/wp-content/uploads/2021/10/Moby-logo-sm.png" width=80> Startup

#### _Per avviare una istanza del config-service con postgres locale_

Il task-scheduler-service può essere facilmente installato via docker compose su server Linux utilizzando il file 
docker-compose.yml presente in questo repository.

Accertati di aver installato docker e il plugin di docker `compose` dove vuoi installare il config-service e in seguito
esegui il comando successivo per un setup di esempio.

```
curl -fsSL https://raw.githubusercontent.com/trasparenzai/task-scheduler-service/main/first-setup.sh -o first-setup.sh && sh first-setup.sh
```

Collegarsi a http://localhost:8082/swagger-ui/index.html per visualizzare la documentazione degli endpoint REST presenti nel servizio.

## 👏 Come Contribuire 

E' possibile contribuire a questo progetto utilizzando le modalità standard della comunità opensource 
(issue + pull request) e siamo grati alla comunità per ogni contribuito a correggere bug e miglioramenti.

## 📄 Licenza

Task Scheduler Service è concesso in licenza GNU AFFERO GENERAL PUBLIC LICENSE, come si trova nel file
[LICENSE][l].

[l]: https://github.com/trasparenzai/public-sites-service/blob/master/LICENSE
