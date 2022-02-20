<template>
  <div class="background">
    <span class="school">端口代理系统</span>
    <div style="float:right;margin-right: 10px">
      <span class="name">{{ name }}</span>
      <a-avatar class="avatar" :size='40' :src="portrait"></a-avatar>
    </div>
  </div>
</template>

<script>
export default {
  name: "HeadBar",
  computed: {
    portrait() {
      return 'https://s2.loli.net/2022/02/19/FBGyahle6J4Twpo.png'
    },
    name() {
      return 'admin'
    }
  },
}
</script>

<style scoped>
.background {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: 60px;
  background-image: linear-gradient(to right, #4858C9, #4959C8, #6170C3, #6170C3);
}

.school {
  float: left;
  margin-left: 20px;
  line-height: 60px;
  font-size: 20px;
  -webkit-text-fill-color: #ffffff;
  -webkit-text-stroke: 0.8px #e1e3f5;
}
.school:hover {
  cursor: default;
}

.name {
  float: right;
  margin-right: 20px;
  line-height: 60px;
  font-size: 15px;
  -webkit-text-fill-color: #ffffff;
  -webkit-text-stroke: 0.5px #e1e3f5;
}

.name:hover {
  cursor: default;
}

.avatar {
  margin-top: 10px;
  margin-right: 15px;
}
</style>
