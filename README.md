# EyeCanTalk

> 아이트래킹을 활용한 보완대체의사소통 앱

장애인 플러스 기술 공모전 참여 작품

## 시연 영상

아이트래킹을 통해 초점의 움직임을 감지하고, 눈깜빡임으로 해당 시점의 초점 좌표에 강제 클릭 이벤트 발생시켜 손의 사용없이 눈으로만 애플리케이션 사용
![Alt text](%EC%9D%98%EC%82%AC%EC%86%8C%ED%86%B51.gif)
![Alt text](%EC%9D%98%EC%82%AC%EC%86%8C%ED%86%B51-1.gif)

## 사용 기술 스택

<div align=center>
   <img src="https://img.shields.io/badge/androidstudio-3DDC84?style=for-the-badge&logo=androidstudio&logoColor=white">
   <img src="https://img.shields.io/badge/java-007396?style=for-the-badge&logo=java&logoColor=white">
   <img src="https://img.shields.io/badge/python-3776AB?style=for-the-badge&logo=python&logoColor=white">
   <img src="https://img.shields.io/badge/jupyter-F37626?style=for-the-badge&logo=jupyter&logoColor=white">
   <img src="https://img.shields.io/badge/github-181717?style=for-the-badge&logo=github&logoColor=white">
   <img src="https://img.shields.io/badge/amazonaws-232F3E?style=for-the-badge&logo=amazonaws&logoColor=white">
   <img src="https://img.shields.io/badge/tensorflow-FF6F00?style=for-the-badge&logo=tensorflow&logoColor=white">
   <img src="https://img.shields.io/badge/flask-000000?style=for-the-badge&logo=flask&logoColor=white">
 
</div>

## 프로젝트 개요

### 개발 기간 및 참여 인원

- 2023년 7월 20일 ~ 10월 2일
- 3명

### 제안 배경

- AAC : 말과 언어 표현 및 이해에 크고 작은 장애를 보이는 사람들에게 의사소통을 할 수 있는 기회를 주고 의사소통 능력을 향상시키도록 말을 보완하거나(augment) 대체적인(alternative) 방법을 사용하는 것

![image](https://github.com/LifeLikeMine/EyeCanTalk/assets/84857521/1fbb8e50-25cd-46da-bc14-43b4f127a8f5)

- 아이트래킹 기법 : 최소의 운동성으로 최대의 효과를 내는 기법으로 국외에서 AAC 사용 기법 중 하나로 적극 사용되고 AAC 훈련을 통해 많은 지체중복 장애학생의 교육에 적극 활용

하지만 국내 에선 아이트래커 기기의 비용이 비싸고 개념도 부족하기 때문에 수입 및 적용도 전혀 없는 상황이다.

논문에 따르면 만약 아이트래킹 기법 을 적용 한다면 기존의 평가불능으로 나타났던 많은 지체중복장애 학생의 교육과 평가에 적극 활용될 것으로 예상된다.

또 AAC 기기에서 원하는 요소를 선택하는데 시간이 오래 걸리기 때문에 대화를 주고받는다고 느낄수 있는 만큼의 효율적인 AAC 기기나 보조공학이 부족하다.

### 프로젝트 목표

- 아이트래킹 : 아이트래킹 SDK  를 활용해 별도의 아이트래커 없이 아이트래킹 기능 구현
- AAC 추천 모델 : 사용자의 사용 기록을 분석하고 다음 선택할 AAC 추천, 입력시간을 단축

## 시스템 구성도

![image](https://github.com/LifeLikeMine/EyeCanTalk/assets/84857521/43b58e75-df66-4ece-be3e-cac5100fbe3d)

## 화면 구성

### 메인 화면 구성도

![image](https://github.com/LifeLikeMine/EyeCanTalk/assets/114338420/bd13fba3-5ebe-4a5f-b2b2-83b1fcb90f18)

### 설정 화면 구성도

![image](https://github.com/LifeLikeMine/EyeCanTalk/assets/114338420/d08a94c3-3f20-4c49-b831-4a6583311b92)

## 주요 기능

## 아이트래킹 어플리케이션

Seeso SDK를 이용한 아이트래킹 기능 개발

- Android studio에서도 사용가능한 Seeso SDK를 이용하여 아이트래킹 기능과 터치 기능 개발
- 이에 맞는 AAC 목록 및 카테고리 삽입

## AI

GRU 를 활용한 AAC 상징 추천 모델 개발

- AAC 상징 시퀀스 데이터 의 마지막 값을 Y 나머지 값을 X로 분리
- 데이터 전처리 (pad_sequence, 원핫 인코딩)
- Embedding -> GRU -> Dense 레이어 구조

#### 학습 데이터

- 사용자가 앱을 사용하면서 쌓인 AAC 상징 시퀀스 데이터

#### 학습 결과

![image](https://github.com/LifeLikeMine/EyeCanTalk/assets/84857521/a10f5ee1-8443-4f13-b6d1-47436624450e)
