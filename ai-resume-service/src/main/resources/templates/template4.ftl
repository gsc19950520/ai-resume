<!DOCTYPE html>
<html lang="zh-cn">
<head>
<meta charset="UTF-8" />
<title>模板四简历</title>
<style>
  body {
    margin: 0;
    padding: 0;
    background: #f5f5f5;
    font-family: "PingFang SC", "Microsoft YaHei", Arial, sans-serif;
  }
  .template-four-container {
    width: 800px;
    min-height: 1120px;
    margin: 0 auto;
    background: #fff;
    display: flex;
    padding: 15px 5px;
    box-sizing: border-box;
  }
  .left-panel {
    width: 30%;
    min-height: 100%;
    background: rgba(20, 130, 145, 0.56);
    border-radius: 10px;
    padding: 12px;
    color: #fff;
    box-shadow: 0 3px 6px rgba(0,0,0,0.2);
    display: flex;
    flex-direction: column;
    align-items: center;
  }
  .avatar-container {
    text-align: center;
    margin-bottom: 20px;
  }
  .avatar {
    width: 70px;
    height: 70px;
    border-radius: 50%;
    border: 2px solid rgba(255,255,255,0.8);
    margin-bottom: 6px;
  }
  .name { font-size: 18px; font-weight: bold; }
  .title { font-size: 13px; opacity: 0.9; }
  .contact-section { width: 100%; margin-top: 15px; }
  .section-title { font-size: 14px; font-weight: 600; margin-bottom: 10px; border-bottom: 1px solid rgba(255,255,255,0.5); padding-bottom: 4px; }
  .contact-item { display: flex; font-size: 12px; margin-bottom: 6px; line-height: 1.2; }
  .contact-label { font-weight: 500; white-space: nowrap; margin-right: 4px; }
  .contact-value { word-break: break-word; flex: 1; }
  .skills-section { width: 100%; margin-top: 40px; }
  .skill-item { margin-bottom: 12px; }
  .skill-name { font-size: 12px; margin-bottom: 4px; }
  .skill-bar { width: 100%; height: 6px; background: rgba(255,255,255,0.3); border-radius: 6px; overflow: hidden; }
  .skill-fill { height: 100%; background: #fff; }
  .right-panel { width: 70%; padding-left: 20px; display: flex; flex-direction: column; gap: 15px; }
  .section { background: #fff; border-radius: 8px; padding: 12px; box-shadow: 0 2px 6px rgba(0,0,0,0.1); }
  .job { margin-bottom: 15px; }
  .job-header { display: flex; justify-content: space-between; font-size: 13px; font-weight: 600; margin-bottom: 6px; }
  .job-desc { font-size: 12px; color: #444; line-height: 1.4; position: relative; padding-left: 10px; }
  .job-desc::before { content: "•"; position: absolute; left: 0; top: 0; color: #000; font-size: 14px; font-weight: bold; }
</style>
</head>
<body>

<div class="template-four-container">
  <div class="left-panel">
    <div class="avatar-container">
      <img src="${personalInfo.avatar!''}" class="avatar" />
      <div class="name">${personalInfo.name!"姓名"}</div>
      <div class="title">${personalInfo.jobTitle!''}</div>
    </div>

    <div class="contact-section">
      <div class="section-title">个人信息</div>
      <div class="contact-item"><span class="contact-label">电话：</span><span class="contact-value">${personalInfo.phone!''}</span></div>
      <div class="contact-item"><span class="contact-label">邮箱：</span><span class="contact-value">${personalInfo.email!''}</span></div>
      <div class="contact-item"><span class="contact-label">地址：</span><span class="contact-value">${personalInfo.address!''}</span></div>
      <div class="contact-item"><span class="contact-label">出生日期：</span><span class="contact-value">${personalInfo.birthDate!''}</span></div>
      <div class="contact-item"><span class="contact-label">期望薪资：</span><span class="contact-value">${personalInfo.expectedSalary!''}</span></div>
      <div class="contact-item"><span class="contact-label">入职时间：</span><span class="contact-value">${personalInfo.startTime!''}</span></div>
    </div>

    <div class="skills-section">
      <div class="section-title">专业技能</div>
      <#list skills as skill>
        <div class="skill-item">
          <div class="skill-name">${skill.name!''}</div>
          <div class="skill-bar">
            <div class="skill-fill" style="width: ${skill.level!0}%"></div>
          </div>
        </div>
      </#list>
    </div>
  </div>

  <div class="right-panel">
    <div class="section">
      <div class="section-title">教育经历</div>
      <#list education as edu>
        <div class="job">
          <div class="job-header">
            <div>${edu.school!''}</div>
            <div>${edu.degree!''}</div>
            <div>${edu.startDate!''} - ${edu.endDate!''}</div>
          </div>
          <div class="job-desc">${edu.major!''}</div>
        </div>
      </#list>
    </div>

    <div class="section">
      <div class="section-title">工作经历</div>
      <#list workExperience as work>
        <div class="job">
          <div class="job-header">
            <div>${work.company!''}</div>
            <div>${work.position!''}</div>
            <div>${work.startDate!''} - ${work.endDate!''}</div>
          </div>
          <div class="job-desc">${work.description!''}</div>
        </div>
      </#list>
    </div>

    <div class="section">
      <div class="section-title">项目经验</div>
      <#list projectExperience as proj>
        <div class="project">
          <div class="job-header">
            <div>${proj.name!''}</div>
            <div>${proj.startDate!''} - ${proj.endDate!''}</div>
          </div>
          <div class="job-desc">${proj.description!''}</div>
        </div>
      </#list>
    </div>

    <div class="section">
      <div class="section-title">兴趣爱好</div>
      <div class="job-desc">${personalInfo.interests!''}</div>
    </div>

    <div class="section">
      <div class="section-title">自我评价</div>
      <div class="job-desc">${personalInfo.selfEvaluation!''}</div>
    </div>
  </div>
</div>

</body>
</html>
