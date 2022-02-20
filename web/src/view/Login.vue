<template>
  <div class="login-box">
    <h2 class="login-title">端口代理系统</h2>
    <a-input-password placeholder="input password" v-model="password"/>
    <a-button type="primary" class="button" @click="login">
      登录
    </a-button>
  </div>
</template>

<script>
export default {
  name: "Login",
  data() {
    return {
      password: '123456'
    }
  },
  methods: {
    login() {
      if (this.password === '123456') {
        this.$router.push({
              name: 'Main'
            }
        )
      } else {
        this.$message.error("密码错误")
      }
    }
  }
}
</script>

<style scoped>
.login-box {
  border: 1px solid #DCDFE6;
  width: 300px;
  margin: 180px auto;
  padding: 35px 35px 15px 35px;
  border-radius: 5px;
  -webkit-border-radius: 5px;
  -moz-border-radius: 5px;
  box-shadow: 0 0 25px paleturquoise;
}

.login-title {
  text-align: center;
  margin: 0 auto 40px auto;
  color: #040707;
  font-size: 25px;
  font-weight: bold;
  letter-spacing: 2px;
  padding: 14px 0;
}

.button{
  margin-top: 40px;
  margin-left: 36%;
  margin-bottom: 30px;
}
</style>
