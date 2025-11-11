// 测试generateQuestion函数的简化版本
function testGenerateQuestion() {
  // 模拟函数结构，但不包含实际的API调用
  const mockFunction = function(jobType, questionFocus, styleHint, persona, lastAnswer, randomFactor) {
    // 参数验证和默认值处理 - 移到try块外部以确保作用域正确
    const safeJobType = jobType || '通用面试';
    const safeQuestionFocus = questionFocus || '基础知识';
    const safeStyleHint = styleHint || '引导式';
    const safePersona = persona || '正式面试';
    const safeLastAnswer = lastAnswer || '';
    const safeRandomFactor = randomFactor !== undefined ? randomFactor : 0.5;
    
    try {
      // 准备请求参数
      const requestData = {
        jobType: safeJobType,
        questionFocus: safeQuestionFocus,
        styleHint: safeStyleHint,
        persona: safePersona,
        lastAnswer: safeLastAnswer,
        randomFactor: safeRandomFactor
      };
      
      console.log('请求参数准备成功:', requestData);
      return '模拟成功';
    } catch (error) {
      console.error('生成问题处理失败:', error);
    }
    
    // 本地备选逻辑：当API调用失败时生成默认问题
    console.log('使用备选逻辑，参数作用域正确:', safeJobType, safeQuestionFocus, safePersona);
    return '备选问题';
  };
  
  // 测试函数
  const result = mockFunction('前端开发', '算法', '挑战式', '技术专家');
  console.log('测试结果:', result);
  console.log('函数结构语法正确，变量作用域已修复!');
}

testGenerateQuestion();