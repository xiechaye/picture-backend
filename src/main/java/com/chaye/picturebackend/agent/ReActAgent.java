package com.chaye.picturebackend.agent;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class ReActAgent extends BaseAgent {

    /**
     * 处理当前状态，并决定是否需要行动
     *
     * @return 是否需要行动
     */
    public abstract boolean think();

    /**
     * 执行决定行动
     *
     * @return 执行结果
     */
    public abstract String act();

    /**
     * 先思考再执行
     *
     * @return 执行结果
     */
    @Override
    public String step() {
        try {
            boolean shouldAct = think();
            if(!shouldAct) {
                return "思考完成，不需要行动";
            }
            return act();
        } catch (Exception e) {
            log.error(e.getMessage());
            return "步骤执行失败: " + e.getMessage();
        }
    }
}
