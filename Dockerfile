FROM adoptopenjdk:14-jre

ENV MONGODB_CONNECTION_STRING="mongodb://host.docker.internal:27017/admin?ssl=false"

COPY cajuthorizer-1.0-SNAPSHOT /cajuthorizer-1.0-SNAPSHOT

EXPOSE 80

CMD /cajuthorizer-1.0-SNAPSHOT/bin/cajuthorizer -Dhttp.port=80 -Dplay.http.secret.key='J6srUVRhQE_f1AIzEmC5QPPIAU81HvF]SKaB57phIIHrbpZ]uzSSV>mn;W9cTBx='