![image](https://user-images.githubusercontent.com/34666301/132111872-521e5056-8361-46ee-8983-eb142b034de6.png)

## 개요 
우주지상국 서브시스템들과 1:1 통신을 위한 Spring, Netty기반 TCP 클라이언트 공통([TcpClient.java](https://github.com/Jsing/netty-tcp-client/blob/main/src/main/java/netty/tcp/client/TcpClient.java)) 모듈을 개발하였습니다. Netty 프레임워크는 기존의 POJO(Plain Old Java Object)기반의 Socket 라이브러리를 사용하는 것과 비교해 다음과 같은 훌륭한 가치를 제공한다고 판단하여 선택하였습니다.

<br/>

## 주요 API 
- 연결, 연결 해제
- 장비와 연결 성공할 때 까지 비동기 연결 반복 시도
- 장비와 연결 끊어질 경우 자동 연결 복구 로직
- 통신 관련 사용자 태스크 관리
- 연결 상태 알람 

<br/>


## Netty 프레임워크를 선택한 이유
기존에 개발된 TCP 클라이언트 공통 모듈(POJO 소켓 기반)의 복잡성과 낮은 가독성 때문에 새로운 모듈 개발에 대한 지속적인 요구가 팀내에서 있었습니다. Netty 프레임워크는 다음과 같이 훌륭한 가치를 제공한다고 판단했습니다. 


### 1. 구조적 프로그래밍 가이드
Netty 프레임워크의 `Pipeline` 아키텍처는 개발자가 구조적인 프로그래밍을 할 수 있도록 돕습니다. 예를 들면 다음과 같은 TCP 채널의 수신 처리 로직들이 개별적인 `Handler`로 구현되고 `Pipeline`에 의해 구조화 될 수 있습니다. 

![image](https://user-images.githubusercontent.com/34666301/127424496-299bbc5d-19e7-494b-824d-6c121887c822.png)
```
1. 메시지 수신  
2. 메시지 바운더리 구분
3. 메시지 포멧 변환 
4. 메시지 해석 
5. 메시지 검증 
6. 메시지 로깅 
7. 메시지 전달
```


### 2. 높은 확장성 
역시 `Pipeline` 아키텍처로 인하여 통신 처리 기능들을 확장하기 용이합니다. 그래서 소프트웨어를 `점진적으로 확장`해 나갈 수 있습니다. 예를 들면 `최소 기능 제품(MVP)`을 빠르게 만들어야 하는 경우 핵심 기능만 빠르게 만들고, 안정화 및 고도화된 기능들은 추후에 Pipeline 에 추가하기 쉽습니다. 그래서 초보개발자가 개발한 코드도 추후 개선해 나가고 확장해 나가기 쉽습니다. 

![image](https://user-images.githubusercontent.com/34666301/127424510-a07bdc88-75ff-4bee-9a71-c0bdc570ca77.png)


```
1. 메시지 수신 
2. 메시지 바운더리 구분 --> 급히 기능만 확인하고 싶은 경우 메시지 바운더리 구분도 추후에 구현할 수 있습니다. 
3. 메시지 포멧 변환 
4. 메시지 해석 
5. 메시지 검증 --> 안정화를 위한 기능임으로 추후 구현할 수 있습니다.
6. 메시지 로깅 --> 선택적인 기능임으로 추후 구현할 수 있습니다. 
7. 메시지 전달
``` 



### 3. 완벽한 수준의 메시지 구분 서비스
TCP는 `스트림기반 프로토콜`이기 때문에 `메시지를 구분`해주는 로직이 사용자 코드에 반드시 추가되어야 합니다. 그러나 많은 개발자들이 위와 같은 필요를 알지 못하고, 일반적인 상황에서만 잘 동작하는 불안정한 코드를 작성하는 경우가 많은 것 같습니다. 

- [Dealing with a Stream-based Transport 절 참조
](https://netty.io/wiki/user-guide-for-4.x.html)

![image](https://user-images.githubusercontent.com/34666301/121465849-55e05a80-c9f1-11eb-9f92-5b479c1b9d2f.png)

Netty 에서는 다양한 형태의 `Frame Decoder`를 제공하기 때문에 위 요구사항을 개발자들이 이해하도록 설득력있게 설명할 수 있고, 구현하기도 무척 쉽습니다. 
- [FixedLengthFrameDecoder](https://netty.io/4.0/api/io/netty/handler/codec/FixedLengthFrameDecoder.html)
- [LengthFieldBasedFrameDecoder](https://netty.io/4.0/api/io/netty/handler/codec/LengthFieldBasedFrameDecoder.html)
- [DelimiterBasedFrameDecoder](https://netty.io/4.0/api/io/netty/handler/codec/DelimiterBasedFrameDecoder.html)


### 4. 수신 쓰레드 서비스 
메시지 수신을 위한 별도의 쓰레드를 생성하고 관리하지 않아도 Pipeline 과 연계되는 `EventLoop` 서비스를 제공합니다. 


### 5. 안전한 종료 서비스 
대게 수신 쓰레드를 안전하게 종료하기 위하여 이벤트 객체 (또는 플래그)와 수신 루프상에 체크 로직을 필요로 합니다. `EventLoop.shutdownGracefully()` 서비스를 사용해 사용자 코드에서 위와 같은 로직을 제거하고, 통신 코드 자체에 집중할 수 있습니다.   
