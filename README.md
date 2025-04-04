# Lista de Tarefas - Android

Este é um aplicativo de gerenciamento de tarefas desenvolvido em **Jetpack Compose** com **Firebase Authentication** e **Firestore** para armazenamento de dados em tempo real.

## Recursos
- Cadastro e login de usuários com Firebase Authentication
- Adicionar, editar e excluir tarefas
- Marcar tarefas como concluídas
- Logout do usuário

## Configuração e Execução

### 1. Clonar o repositório
```sh
git clone https://github.com/seu-usuario/lista-de-tarefas.git
cd lista-de-tarefas
```

### 2. Configurar o Firebase
1. Acesse [Firebase Console](https://console.firebase.google.com/)
2. Crie um novo projeto
3. Adicione um aplicativo Android e configure o **google-services.json**
4. Habilite **Authentication** (Email/Senha) e **Firestore Database**
5. Copie o arquivo `google-services.json` para `app/` no projeto

### 3. Executar o projeto
1. Abra o projeto no **Android Studio**
2. Conecte um emulador ou dispositivo físico
3. Clique em **Run** (ou use `Shift + F10`)

## Estrutura do Projeto
- `MainActivity.kt` - Gerencia a autenticação e a navegação
- `AuthScreen.kt` - Tela de login/cadastro
- `TaskScreen.kt` - Tela de listagem e gerenciamento de tarefas

## Dependências Principais
```gradle
implementation 'com.google.firebase:firebase-auth-ktx'
implementation 'com.google.firebase:firebase-firestore-ktx'
implementation 'androidx.compose.material3:material3:1.0.0'
```

## Licença
Este projeto está sob a licença MIT. Sinta-se à vontade para contribuir!

