# Task Scheduler Service del progetto TrasparenzAI
## Task Scheduler Service - Servizi programmati a tempo

[![Supported JVM Versions](https://img.shields.io/badge/JVM-11-brightgreen.svg?style=for-the-badge&logo=Java)](https://openjdk.java.net/install/)

Task Scheduler Service è parte della suite di servizi per la verifica delle informazioni sulla
Trasparenza dei siti web delle Pubbliche amministrazioni italiane.
 
## Task Scheduler Service

Task Scheduler Service è il componente che si occupa di avviare alcuni processi eseguiti a intervalli fissi, come
per esempio l'avvio delle scansioni dei siti del PA per la verifica della corrispondenza dei requisiti.

Nell'utilizzo tramite **docker-compose.yml** ricordarsi di impostare la corretta variabile d'ambiente che specifica
l'url del config-service da utilizzare.

```
  environment:
    - spring.config.import=optional:configserver:https://dica33.ba.cnr.it/config-service/config
```

## 👏 Come Contribuire 

E' possibile contribuire a questo progetto utilizzando le modalità standard della comunità opensource 
(issue + pull request) e siamo grati alla comunità per ogni contribuito a correggere bug e miglioramenti.

## 📄 Licenza

Task Scheduler Service è concesso in licenza GNU AFFERO GENERAL PUBLIC LICENSE, come si trova nel file
[LICENSE][l].

[l]: https://github.com/cnr-anac/public-sites-service/blob/master/LICENSE
