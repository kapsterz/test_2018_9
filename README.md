# SLA Throttling Service

Implemented as test issue. 

# Rules for RPS counting

  1. If no token provided, assume the client as unauthorized.
  2. All unauthorized user's requests are limited by GraceRps
  3. If request has a token, but slaService has not returned any info yet,
  treat it as unauthorized user
  4. RPS should be counted per user, as the same user might use
  different tokens for authorization
  5. SLA should be counted by intervals of 1/10 second (i.e. if RPS
  limit is reached, after 1/10 second ThrottlingService should allow
  10% more requests)
  6. SLA information is changed quite rarely and SlaService is quite
  costly to call (~250ms per request), so consider caching SLA requests. Also, you should not query the service, if the same token request is already in progress.
  7. Consider that REST service average response time is bellow 5ms, ThrottlingService shouldn’t impact REST service SLA.

# How to Run

Just exec in console

    $ sbt slaServiceApi/run

Or for running tests
    
    $ sbt slaServiceApi/test
    
After that in port 9000 will started Throttling Service API and on port 8118 will started LoadTest API

# Main routes for Throttling Service API
    
    #Endpoint with Throtling Service Executing
    #Token is optional, when token empty or does't exists user is unauthorized
    GET        /api/testSla?token=token
    #Endpoint without Throtling Service Executing for perfomance comparing
    #Token is optional, when token empty or does't exists user is unauthorized
    GET        /api/test     
    #Ensuring Caches route. Doesnt kill UserActors which already created           
    GET        /api/ensureCaches        com.osmolovskyi.test.controllers.MainController.ensureCaches
    
# Main routes for LoadTest API
    
    #Starting executing loadtest
    GET        /api/start

# About performance

Service relates from SlaService which return response after 250ms by default.

So according to reports:
    
    While using clean REST API: 
    
    {"requestsCount":381416,"requestsSuccessful":381416,"requestsFailed":0,"requestsAuthorizedFailed":0,"requestsAuthorizedSuccessful":190172,"requestsUnauthorizedFailed":0,"requestsUnauthorizedSuccessful":191244,"httpErrorsCount":0,"duration":45,"requestsPerSecond":8475}
    
    While using TS API in first time: 
    
    {"requestsCount":285264,"requestsSuccessful":278283,"requestsFailed":6981,"requestsAuthorizedFailed":4444,"requestsAuthorizedSuccessful":138065,"requestsUnauthorizedFailed":2537,"requestsUnauthorizedSuccessful":140218,"httpErrorsCount":0,"duration":45,"requestsPerSecond":6339}
    
    While using TS API in second time whithout ensuring caches:
    
    {"requestsCount":431569,"requestsSuccessful":430866,"requestsFailed":703,"requestsAuthorizedFailed":699,"requestsAuthorizedSuccessful":214954,"requestsUnauthorizedFailed":4,"requestsUnauthorizedSuccessful":215912,"httpErrorsCount":0,"duration":45,"requestsPerSecond":9590}
    
Result can changed time to time in reason of hardware throttling, but expected performance percentage between clean REST and TS APIs is around 70% in first time and 85% in second
In tests was used as default values
    
    Duration of test:       45 seconds
    RPS per User:           10
    Grace RPS:              1000
    Users count:            1000
    SLA Response Timeout:   250 millis