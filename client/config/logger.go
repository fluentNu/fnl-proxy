package config

import (
	"fmt"
	"go.uber.org/zap"
	"go.uber.org/zap/zapcore"
	"os"
)

// InitLogger 日志配置
func InitLogger(s string) *zap.Logger {
	writeSyncer := getLogWriter(s)
	encoder := getEncoder()
	core := zapcore.NewCore(encoder, writeSyncer, zapcore.DebugLevel)

	logger := zap.New(core)
	return logger
}

func getEncoder() zapcore.Encoder {
	return zapcore.NewJSONEncoder(zap.NewProductionEncoderConfig())
}

func getLogWriter(s string) zapcore.WriteSyncer {
	file, _ := os.Create(fmt.Sprintf("./%s.log", s))
	return zapcore.AddSync(file)
}
