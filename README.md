# Android bluethoot Server Client communication 

###### last changes:
- Server / Client / Communication threads separated  
- fix socket connection failure
- Close redundant threads after opening socket
- Send first command

###### todos:
- Start measure command before closing Client thread and listen in new thread 

###### functions:
- [x] list paired device
- [x] pair with discovered devices 
- [x] select paired device for communication
- [x] start communication channel as server
- [x] connect communication channel as client
- [x] send custom messages 
