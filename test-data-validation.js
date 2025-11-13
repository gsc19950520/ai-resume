// 测试数据验证脚本
// 用于验证技能数据在保存和加载过程中的正确性

// 模拟用户提供的保存时数据
const testData = {
  "title":"我的新简历",
  "personalInfo":{
    "name":"高先生",
    "jobTitle":"软件开发",
    "phone":"17855555555",
    "email":"28754@qq.com",
    "address":"北京市朝阳区",
    "birthDate":"2000.05",
    "expectedSalary":"12120",
    "startTime":"一周内",
    "interests":["打篮球"]
  },
  "education":[{"id":1,"school":"北京大学","major":"计算机软件","degree":"本科","startDate":"2015","endDate":"2019"}],
  "workExperience":[{"id":1,"company":"深圳科技网络","position":"java开发","startDate":"2017","endDate":"至今","description":"写代码阿达打撒打撒夫人翁帆士大夫VS光和热通过"}],
  "skills":["js","java1"],
  "skillsWithLevel":[{"name":"js","level":4},{"name":"java1","level":2}],
  "selfEvaluation":"认真"
};

// 模拟数据标准化函数
function normalizeResumeData(rawData, templateId) {
  console.log(`\n=== 开始标准化数据 (模板: ${templateId}) ===`);
  console.log('原始数据:', JSON.stringify(rawData, null, 2));
  
  if (!rawData) {
    console.log('❌ 原始数据为空');
    return null;
  }
  
  const normalizedData = {};
  
  // 根据模板ID添加特定字段
  switch (templateId) {
    case 'template-four':
    case 'template-five':
    case 'template-six':
      console.log('📋 处理 template-four/five/six 格式');
      
      // 技能格式处理
      console.log('\n🔧 技能数据处理:');
      console.log('- rawData.skills:', rawData.skills);
      console.log('- rawData.skillsWithLevel:', rawData.skillsWithLevel);
      console.log('- rawData.skills 类型:', typeof rawData.skills);
      console.log('- rawData.skillsWithLevel 类型:', typeof rawData.skillsWithLevel);
      
      const skillsData = rawData.skills || rawData.skillsWithLevel || [];
      console.log('- 最终使用的 skillsData:', skillsData);
      console.log('- skillsData 长度:', skillsData.length);
      
      normalizedData.skills = skillsData.map((item, index) => {
        console.log(`  处理技能项 ${index}:`, item);
        let processedItem;
        
        if (typeof item === 'string') {
          // 处理字符串格式的技能（如 skills: ["js", "java1"]）
          processedItem = {
            name: item,
            level: 80 // 默认80%熟练度
          };
          console.log(`  字符串技能 -> 对象: ${item} ->`, processedItem);
        } else if (typeof item === 'object' && item !== null) {
          // 处理对象格式的技能（如 skillsWithLevel: [{"name":"js","level":4}]）
          processedItem = {
            name: item.name || item.skillName || '',
            level: item.level || item.proficiency || 80
          };
          console.log(`  对象技能 -> 标准化:`, processedItem);
        } else {
          // 处理其他情况
          processedItem = {
            name: '',
            level: 80
          };
          console.log(`  未知格式 -> 默认:`, processedItem);
        }
        
        return processedItem;
      });
      
      console.log('最终生成的 skills:', normalizedData.skills);
      break;
      
    case 'template-one':
    default:
      console.log('📋 处理 template-one 和其他模板格式');
      normalizedData.skillsWithLevel = normalizeSkillsData(rawData);
      normalizedData.skills = normalizedData.skillsWithLevel;
      break;
  }
  
  return normalizedData;
}

// 辅助函数：标准化技能数据
function normalizeSkillsData(rawData) {
  console.log('\n🔧 normalizeSkillsData 函数:');
  console.log('- 原始数据:', rawData);
  console.log('- rawData.skills:', rawData.skills);
  console.log('- rawData.skillsWithLevel:', rawData.skillsWithLevel);
  
  const skills = rawData.skills || rawData.skillsWithLevel || [];
  console.log('- 最终使用的 skills:', skills);
  console.log('- skills 类型:', typeof skills);
  console.log('- skills 是数组吗:', Array.isArray(skills));
  
  const result = skills.map((item, index) => {
    console.log(`处理技能项 ${index}:`, item);
    let processed;
    
    if (typeof item === 'string') {
      processed = {
        name: item,
        level: 50 // 默认50%熟练度
      };
      console.log(`字符串技能 -> 对象: ${item} ->`, processed);
    } else if (typeof item === 'object' && item !== null) {
      processed = {
        name: item.name || item.skillName || '',
        level: item.level || item.proficiency || 50
      };
      console.log(`对象技能 -> 标准化:`, processed);
    } else {
      processed = {
        name: '',
        level: 50
      };
      console.log(`未知格式 -> 默认:`, processed);
    }
    
    return processed;
  });
  
  console.log('normalizeSkillsData 最终结果:', result);
  return result;
}

// 测试不同的模板
console.log('🚀 开始测试数据标准化流程');
console.log('='.repeat(60));

const templates = ['template-four', 'template-five', 'template-six', 'template-one'];

templates.forEach(templateId => {
  console.log(`\n📝 测试模板: ${templateId}`);
  const result = normalizeResumeData(testData, templateId);
  console.log('最终结果:', JSON.stringify(result, null, 2));
  console.log('-'.repeat(60));
});

console.log('\n✅ 测试完成');

// 分析问题
console.log('\n🔍 问题分析:');
console.log('根据测试结果，可能的问题:');
console.log('1. 如果 rawData.skills 存在且是字符串数组，应该能正确处理');
console.log('2. 如果 rawData.skillsWithLevel 存在且是对象数组，应该能正确处理');
console.log('3. 问题可能在数据保存或获取阶段，而不是标准化阶段');
console.log('4. 需要验证 wx.setStorageSync 和 wx.getStorageSync 的数据完整性');